package tw.edu.nccu.jsqlparser;

import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.create.table.CreateTable;

public class CreateTableTest {
	public static void main(String[] args) throws JSQLParserException {
		CCJSqlParserManager pm = new CCJSqlParserManager(); 
		List<?> columnDefinitions; 
		List<?> tableOptionsStrings; 
		List<?> indexes; 
		String sql3 = "CREATE TABLE Customer ("
				+ "cid INT NOT NULL UNIQUE,"
				+ "oid VARCHAR(100) NOT NULL UNIQUE,"
				+ "name VARCHAR(100) NOT NULL,"
				+ "sex VARCHAR(10) NOT NULL,"
				+ "phone VARCHAR(20) NOT NULL"
				+ "CONSTRAINT pkey PRIMARY KEY (cid),"
				+ "CONSTRAINT cfkey FOREIGN KEY (oid) REFERENCES Order(Oid) "
				+ "ON DELETE CASCADE ON UPDATE CASCADE"
				+ ")"; 
		String sql4 = "CREATE TABLE Customer ("
				+ "cid INT NOT NULL UNIQUE,oid VARCHAR(100) NOT NULL UNIQUE,"
				+ "name VARCHAR(100) NOT NULL,"
				+ "sex VARCHAR(10) NOT NULL,"
				+ "phone VARCHAR(20) NOT NULL,"
				+ "PRIMARY KEY (cid)"
				+ ")"; 
		String sql5 = "TenantId = 100"
				+ "CREATE TABLE Customer ("
				+ "cid INT NOT NULL UNIQUE,oid VARCHAR(100) NOT NULL UNIQUE,"
				+ "name VARCHAR(100) NOT NULL,"
				+ "sex VARCHAR(10) NOT NULL,"
				+ "phone VARCHAR(20) NOT NULL,"
				+ "PRIMARY KEY (cid)"
				+ ")";  
		System.out.println(sql5.substring(sql5.indexOf("CREATE")));
		CreateTable jsqlcreateTable = (CreateTable) pm.parse(new StringReader(sql4));
		columnDefinitions = jsqlcreateTable.getColumnDefinitions(); 
//		tableOptionsStrings = jsqlcreateTable.getTableOptionsStrings(); 
		indexes = jsqlcreateTable.getIndexes(); 
		Iterator<?> columnItr = columnDefinitions.iterator();
//		Iterator<?> tableOptStrItr = tableOptionsStrings.iterator(); 
		Iterator<?> indexesItr = indexes.iterator(); 
		System.out.println("column:"); 
		while (columnItr.hasNext()) {
			System.out.println(columnItr.next().toString());
		}
//		System.out.println("table options:"); 
//		while (tableOptStrItr.hasNext()) {
//			System.out.println(tableOptStrItr.next().toString());
//		}
		System.out.println("indexes:");
		while (indexesItr.hasNext()) {
			String tmp = indexesItr.next().toString();
			String[] lineTmp = tmp.split(" ");
			tmp = lineTmp[2].replace("(", "").replace(")", ""); 
			for (int i=0; i<4; i++) 
				if (indexesItr.hasNext()) 
					indexesItr.next();
			System.out.println(tmp);
		}
		
	}
}
