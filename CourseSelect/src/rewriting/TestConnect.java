package rewriting;

//import java.sql.Connection;
import java.sql.DriverManager;
//import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.*;
//import java.sql.Statement;

import net.sf.jsqlparser.JSQLParserException;


public class TestConnect {
	private static Connection con = null;

	/**
	 * @param args
	 * @throws JSQLParserException
	 *
	 */

	public static void main(String[] args) throws JSQLParserException {
		// TODO Auto-generated method stub

		try {
		      Class.forName("com.mysql.jdbc.Driver");
		      //註冊driver
		      con = (Connection) DriverManager.getConnection(
		      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
		      "root","9326691");
		      //con.getMetaData()

/*
		      DatabaseMetaData metadata = (DatabaseMetaData) con.getMetaData();

		      ResultSet Columns = (ResultSet) metadata.getColumns("", "", "OrderDetailCommonFields", "");

		      while(Columns.next())
		      {
		    	  System.out.println("---------ColumnName:"+Columns.getString(5));
		      }
*/
		      //Statement stmt = (Statement) con.createStatement();
		      //String sql = "SELECT Picture , ItemNo ,TenantName, ItemName , Price FROM ItemInfo WHERE IsClosed = 1 LIMIT 6 OFFSET 0";
		      //String sql = "INSERT INTO OrderDetail (OrderNo,AccountName,ItemNo,Quantity,Date) VALUES (2,'李盈線',1,4,'2012-09-24 14:21:12')";
		      //String sql = "UPDATE ItemInfo SET TenantName = 'Nccu',ItemName = 'Apple',Price = 15,Picture = 'Apple.jpg',IsClosed = 0,Color = 'Red',Color5 = 'null',Sale = 100,Column5 = '122',Column6 = '1321356456',My = '1323654',SaleEnd = 'null',SaleEnd1 = 'null',SaleEnd2 = 'today',NewInt = '120',Test1 = 120,Greeting = 'hello!!!!!',Kevin = 'Kevin23132' WHERE ItemNo = 1";
		      Statement stmt = new Multitenantstatement(con,"ExtensionTable");
		      //String sql = "SELECT *  FROM SelectCourse INNER JOIN CourseInfo ON SelectCourse.CourseId = CourseInfo.CourseId WHERE SelectCourse.StudentId = '99753020' ORDER BY SelectCourse.Priority ASC";
		      //tring sql = "SELECT * FROM CourseInfo WHERE IsClosed = 0";
		      //String sql = "ALTER TABLE OrderDetail ADD Hello Char(20)";
		      //String sql = "SELECT * FROM OrderDetail INNER JOIN ItemInfo ON OrderDetail.ItemNo = ItemInfo.ItemNo";
		      //String sql = "CREATE TABLE TenantPrivateTable (TenantNo integer unique,TenantName char(50) unique,Password char(50) not null,TenantRealName char(50) not null)";
		      //String sql = "CREATE TABLE StudentInfoCommonFields (StudentId Char(50) not null,StudentName Char(50) not null,Password Char(50) not null,Major Char(50) not null, Grade Char(50) not null)";
		      //String sql = "CREATE TABLE CourseInfoCommonFields (CourseId Char(50) not null,CourseName Char(50) not null,Instructors Char(50) not null,Credit Integer not null,Days Char(50) not null,Time Char(50) not null,IsClosed Integer not null)";
		      String sql = "CREATE TABLE SelectCourseCommonFields (SelectId Integer not null,StudentId Char(50) not null,CourseId Char(50) not null,SelectDate datetime not null,Priority Integer not null)";
		      //String sql = "INSERT INTO TenantPrivateTable (TenantNo,TenantName,Password,TenantRealName) VALUES (5,'Happy','9326691','HappyLife')";
		      //String sql = "CREATE TABLE CustomerCommonFields(AccountID int not null,Name char(50))";
		      //String sql = "UPDATE ItemInfo SET Sale = 100";
		      //String sql = "CREATE TABLE CustomerCommonFields (CustomerNo integer not null,AccountName char(50) not null,Password char(50) not null,CustomerName char(50) not null)";
		      //String sql = "CREATE TABLE OrderDetailCommonFields (OrderNo integer not null,AccountName char(50) not null,ItemNo integer not null,Quantity integer not null,Date datetime not null)";
		      //String sql = "CREATE TABLE ItemInfoCommonFields (ItemNo integer not null,TenantName char(50) not null,ItemName char(50) not null,Price integer,Picture char(50),IsClosed integer not null)";
		      //String sql = "INSERT INTO ItemInfo (ItemNo,TenantName,ItemName,Price,Picture,IsClosed) VALUES (1,'Nccu','Apple',15,'1330088585-1967698114 (1).jpg',0)";
		      //String sql = "CREATE TABLE OrderCommonFields(OrderID int not null,AccountID int not null,ItemName char(50) not null,Quantity int not null)";
		      //String sql = "CREATE TABLE ItemInfoCommonFields(ItemID int not null,ItemName char(50) not null,Price int not null)";
		      //String sql = "ALTER TABLE ItemInfo ADD Color char(50)";
		      //String sql = "CREATE TENANT";
		      //String sql = "INSERT INTO CourseInfo (CourseId, CourseName, TeacherName, Location, Credit, Days, Time, Department, IsClosed) VALUES ('NCCU7','XML基本原理','陳正佳','大仁3301',3,'Mon','D56','資訊科學所',0)";
		      //String sql = "ALTER TABLE CustomerCommonFields ADD Name char(30)";
		      //String sql = "ALTER TABLE ItemInfo ADD Column1 char(50)";
		      //String sql = "ALTER TABLE ItemInfo ADD Column2 char(50)";
		      //String sql = "ALTER TABLE ItemInfo ADD Column3 char(50)";
		      //String sql = "ALTER TABLE ItemInfo ADD Column4 char(50)";
		      //String sql = "ALTER TABLE OrderDetail ADD OrderTime DateTime";
		      //String sql = "INSERT INTO Customer (CustomerNo,AccountName,Password,CustomerName) VALUES(2,'John5223',9326691,'John')";
		      //String sql = "INSERT INTO ItemInfo (ItemID,ItemName,Price) VALUES(3,'Orange',25)";
		      //String sql = "INSERT INTO OrderDetail (OrderID,AccountID,ItemName,Quantity,OrderTime) VALUES(3,2,'Banana',15,'2011-01-04 12:00:12')";
		      //String sql = "UPDATE ItemInfo SET ItemName = 'Apple' WHERE ItemNo = 1 ";
		      //String sql = "SELECT AccountName FROM Customer";
		      //String sql = "SELECT * FROM Customer INNER JOIN OrderDetail ON Customer.AccountID = OrderDetail.AccountID INNER JOIN ItemInfo ON OrderDetail.ItemName = ItemInfo.ItemName";
		      //String sql = "DELETE FROM Customer WHERE CustomerNo BETWEEN 2 AND 6";
		      //CreateRewriting Createstm = new CreateRewriting(sql);
		      //AlterRewriting Alterstm = new AlterRewriting(sql,"Nccu");
		      //CreateTenantRewriting CreateTenantstm = new CreateTenantRewriting(sql,"Nccu");
		      //InsertRewriting Insertstm = new InsertRewriting(sql,"Nccu");
		      //UpdateRewriting Updatestm = new UpdateRewriting(sql,"Tenant001");
		      //SelectRewriting Selectstm = new SelectRewriting(sql,"Tenant001");
		      //String sql = "SELECT * FROM Customer WHERE CustomerNo = 1";
		      //String sql = "SELECT Max(TenantNo) FROM TenantPrivateTable";
		      //String sql =

		      //((Multitenantstatement) stmt).settenantId("Nccu");
		      stmt.executeUpdate(sql);
		      //stmt.executeQuery(sql);
		      //ResultSet rs = (ResultSet) stmt.executeQuery(sql);
		      //System.out.print(rs.next());
		      //stmt.settenantId("Happy");

		      /*for test

for(int j = 61;j <= 100;j++)
{
	String TenantName = "Tenant";
    String MyName;
    String MyName2;
    String MyName3;
    String MyName4;
    MyName2 = String.format("%03d", j);
    int ran;
    int ran2;
    int ran3;
    for(int i = 1;i <= 5000;i++)
    {
  	  MyName = String.format("%03d", i);
  	  ran = (int) (Math.random()*500+1);
  	  MyName3 = String.format("%03d", ran);
  	  ran2 = (int) (Math.random()*500+1);
  	  ran3 = (int) (Math.random()*15+1);
  	  //System.out.println(TenantName+MyName+" : "+sql);
  	  //System.out.println(TenantName+MyName);
  	  //String sql = "INSERT INTO Customer (CustomerNo,AccountName,Password,CustomerName) VALUES("+i+",'Account"+MyName+"','Pass"+MyName+"','Customer"+MyName+"')";
  	  String sql = "INSERT INTO OrderDetail (OrderNo,AccountName,ItemNo,Quantity,Date) VALUES("+i+",'Account"+MyName3+"',"+ran2+","+ran3+",'2011-01-04 12:00:12')";
  	  //System.out.println(sql);
  	  stmt.settenantId(TenantName+MyName2);
  	  //System.out.println(TenantName+MyName2+" "+sql);
  	  stmt.executeUpdate(sql);
    }

}




*/

		      //ResultSet rs = (ResultSet) stmt.executeQuery(sql);

		      //System.out.print(rs.next());
		      /*
		      while(Selectstm.finalresult.next())
		      {
		    	  System.out.println(Selectstm.finalresult.getString("AccountName"));
		      }
		      */
		      //DeleteRewriting Selectstm = new DeleteRewriting(sql,"Tenant001");
		      //stmt.execute(Alterstm.getrewritesql());
		      //取得connection

		//jdbc:mysql://localhost/test?useUnicode=true&characterEncoding=Big5
		//localhost是主機名,test是database名
		//useUnicode=true&characterEncoding=Big5使用的編碼

		    }
		    catch(ClassNotFoundException e)
		    {
		      System.out.println("DriverClassNotFound :"+e.toString());
		    }//有可能會產生sqlexception
		    catch(SQLException x) {
		      System.out.println("Exception :"+x.toString());
		    }

	}

}

