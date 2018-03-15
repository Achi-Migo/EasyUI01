package com.easyUI.utils;

	import java.io.File;
import java.io.FileWriter;
	import java.io.IOException;
	import java.io.PrintWriter;
	import java.sql.Connection;
	import java.sql.DriverManager;
	import java.sql.PreparedStatement;
	import java.sql.ResultSetMetaData;
	import java.sql.SQLException;

import javax.servlet.http.HttpServlet;

import jdk.nashorn.internal.ir.RuntimeNode.Request;

	/**
	 * 数据库表转换成javaBean对象小工具(已用了很长时间),
	 *  1 bean属性按原始数据库字段经过去掉下划线,并大写处理首字母等等. 
	 *  2 生成的bean带了数据库的字段说明.
	 *  3 各位自己可以修改此工具用到项目中去.
	 */
	public class GeneratingPojoUtils extends HttpServlet {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String tablename = "";
		private String[] colnames;
		private String[] colTypes;
		private int[] colSizes; // 列名大小
		private int[] colScale; // 列名小数精度
		private boolean importUtil = false;
		private boolean importSql = false;
		private boolean importMath = false;

		/**
		 * @param args
		 */
		public void tableToEntity(String tName) {
			tablename = tName;
			// 数据连Connection获取,自己想办法就行.获取Oracle链接
			//Connection conn = getOracleConnection();
			//获取Mysql链接
			Connection conn=getMysqlConnection();
			//oracle  
			//String strsql = "SELECT * FROM " + tablename+" WHERE ROWNUM=1";// +" WHERE ROWNUM=1"
			//mysql												// 读一行记录;
			String strsql = "SELECT * FROM " + tablename+" limit 1";// +" WHERE ROWNUM=1"
			try {
				System.out.println(strsql);
				PreparedStatement pstmt = conn.prepareStatement(strsql);
				pstmt.executeQuery();
				ResultSetMetaData rsmd = pstmt.getMetaData();
				int size = rsmd.getColumnCount(); // 共有多少列
				colnames = new String[size];
				colTypes = new String[size];
				colSizes = new int[size];
				colScale = new int[size];
				for (int i = 0; i < rsmd.getColumnCount(); i++) {
					rsmd.getCatalogName(i + 1);
					colnames[i] = rsmd.getColumnName(i + 1).toLowerCase();
					colTypes[i] = rsmd.getColumnTypeName(i + 1).toLowerCase();
					colScale[i] = rsmd.getScale(i + 1);
					System.out.println(rsmd.getCatalogName(i + 1));
					if ("datetime".equals(colTypes[i])) {
						importUtil = true;
					}
					if ("image".equals(colTypes[i]) || "text".equals(colTypes[i])) {
						importSql = true;
					}
					if (colScale[i] > 0) {
						importMath = true;
					}
					colSizes[i] = rsmd.getPrecision(i + 1);
				}
				String content = parse(colnames, colTypes, colSizes);
				try {
					FileWriter fw = new FileWriter("src/com/easyUI/bean/"+initcap(tablename) + ".java");
					PrintWriter pw = new PrintWriter(fw);
					pw.println(content);
					pw.flush();
					pw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				releaseConnection(conn);
			}
		}

		/**
		 * 解析处理(生成实体类主体代码)
		 */
		private String parse(String[] colNames, String[] colTypes, int[] colSizes) {
			StringBuffer sb = new StringBuffer();
			sb.append("package com.easyUI.bean;\r\nimport java.io.Serializable;\r\n");
			if (importUtil) {
				sb.append("import java.util.Date;\r\n");
			}
			if (importSql) {
				sb.append("import java.sql.*;\r\n\r\n");
			}
			if (importMath) {
				sb.append("import java.math.*;\r\n\r\n");
			}
			// 表注释
			//processAllAttrs(sb_raw);//使用数据库中原生数据
			processColnames(sb);
			sb.append("public class " + initcap(tablename)
					+ " implements Serializable {\r\n");
			processAllAttrs(sb);
			processAllMethod(sb);
			sb.append("}\r\n");
			System.out.println(sb.toString());
			return sb.toString();

		}

		/**
		 * 处理列名,把空格下划线'_'去掉,同时把下划线后的首字母大写 要是整个列在3个字符及以内,则去掉'_'后,不把"_"后首字母大写.
		 * 同时把数据库列名,列类型写到注释中以便查看,
		 * 
		 * @param sb
		 */
		private void processColnames(StringBuffer sb) {
			sb.append("\r\n/** " + tablename + "\r\n");
			String colsiz = "";
			for (int i = 0; i < colnames.length; i++) {
				colsiz = colSizes[i] <= 0 ? "" : (colScale[i] <= 0 ? "("
						+ colSizes[i] + ")" : "(" + colSizes[i] + "," + colScale[i]
						+ ")");
				sb.append("\t" + colnames[i] + "    "
						+ colTypes[i].toUpperCase() + colsiz + "\r\n");
				char[] ch = colnames[i].toCharArray();
				char c = 'a';
				if (ch.length > 1) {
					for (int j = 0; j < ch.length; j++) {
						c = ch[j];
						if (c == '_') {
							if (ch[j + 1] >= 'a' && ch[j + 1] <= 'z') {
								ch[j + 1] = (char) (ch[j + 1] - 32);
							}
						}
					}
				}
				String str = new String(ch);
				colnames[i] = str.replaceAll("_", "");
			}
			sb.append("*/\r\n");
		}

		/**
		 * 生成所有的方法
		 * 
		 * @param sb
		 */
		private void processAllMethod(StringBuffer sb) {
			for (int i = 0; i < colnames.length; i++) {
				//String upper=firstUpper(colnames[i]);
				sb.append("\tpublic void set"
						+ initcap(colnames[i])
						+ "("
						+ oracleSqlType2JavaType(colTypes[i], colScale[i],
								colSizes[i]) + " " + colnames[i] + "){\r\n");
				sb.append("\t\tthis." + colnames[i] + "=" + colnames[i] + ";\r\n");
				sb.append("\t}\r\n");

				sb.append("\tpublic "
						+ oracleSqlType2JavaType(colTypes[i], colScale[i],
								colSizes[i]) + " get" + initcap(colnames[i])
						+ "(){\r\n");
				sb.append("\t\treturn " + colnames[i] + ";\r\n");
				sb.append("\t}\r\n");
			}
		}

		/**
		 * 解析输出属性
		 * 
		 * @return
		 */
		private void processAllAttrs(StringBuffer sb) {
			sb.append("\tprivate static final long serialVersionUID = 1L;\r\n");
			for (int i = 0; i < colnames.length; i++) {
				sb.append("\tprivate "
						+ oracleSqlType2JavaType(colTypes[i], colScale[i],
								colSizes[i]) + " " + colnames[i] + ";\r\n");
			}
			sb.append("\r\n");
		}

		/**
		 * 把输入字符串的首字母改成大写
		 * 
		 * @param str
		 * @return
		 */
		private String initcap(String str) {
			char[] ch = str.toCharArray();
			if (ch[0] >= 'a' && ch[0] <= 'z') {
				ch[0] = (char) (ch[0] - 32);
			}
			return new String(ch);
		}

		/**
		 * Oracle
		 *数据类型
		 * @param sqlType
		 * @param scale
		 * @return
		 */
		private String oracleSqlType2JavaType(String sqlType, int scale, int size) {
			if (sqlType.equals("integer")) {
				return "Integer";
			}  else if (sqlType.equals("int")) {
				return "int";
			} else if (sqlType.equals("float") || sqlType.equals("float precision"))
				return "float";
			else if (sqlType.equals("double") || sqlType.equals("double precision")) {
				return "Double";
			} else if (sqlType.equals("number") || sqlType.equals("decimal")
					|| sqlType.equals("numeric") || sqlType.equals("real")) {
				return scale == 0 ? (size < 10 ? "Integer" : "Long") : "BigDecimal";
			} else if (sqlType.equals("varchar") || sqlType.equals("varchar2")
					|| sqlType.equals("char") || sqlType.equals("nvarchar")
					|| sqlType.equals("nchar")||sqlType.equals("long")||sqlType.equals("bigint")) {
				return "String";
			} else if (sqlType.equals("datetime") || sqlType.equals("date")
					|| sqlType.equals("timestamp")) {
				return "Date";
			}
			return null;
		}

		/**
		 * 自动获取jcbc链接
		 */
		protected static Connection getOracleConnection() {
			Connection OracleConnection = null;
			try {
				Class.forName("oracle.jdbc.OracleDriver");
				OracleConnection = DriverManager.getConnection(
						"jdbc:oracle:thin:@//218.22.26.142:1522/orcl", "jcfx", "jcfx");
				OracleConnection.setAutoCommit(false);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return OracleConnection;
		}
//获取mysql链接
		protected static Connection getMysqlConnection() {
			Connection MysqlConnection = null;
			try {
				Class.forName("com.mysql.jdbc.Driver");
				MysqlConnection = DriverManager.getConnection(
						"jdbc:mysql://localhost:3306/test01?useUnicode=true&amp;characterEncoding=utf8", "root", "root");
				MysqlConnection.setAutoCommit(false);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return MysqlConnection;
		}
		/**
		 * 释放jdbc链接
		 * 
		 * @param conn
		 */
		protected static void releaseConnection(Connection conn) {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				conn = null;
			}
		}

		/**
		 * @param args
		 */
		public static void main(String[] args) {
			GeneratingPojoUtils t = new GeneratingPojoUtils();
			t.tableToEntity("t_product");
		}

	}