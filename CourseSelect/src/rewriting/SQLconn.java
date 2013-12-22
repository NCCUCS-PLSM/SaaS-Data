package rewriting;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class SQLconn {
	private Connection con = null; //Database objects
	//�s��object
	private Statement stat = null;
	//����,�ǤJ��sql������r��
	private ResultSet rs = null;
	//���G��
	private PreparedStatement pst = null;
	//private String selectSQL = "select accountid,major from tenant31 where accountid=1";

	public SQLconn(){
		try {
	      Class.forName("com.mysql.jdbc.Driver");
	      //���Udriver
	      con = DriverManager.getConnection(
	      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
	      "root","9326691");
	      //���oconnection

	//jdbc:mysql://localhost/test?useUnicode=true&characterEncoding=Big5
	//localhost�O�D���W,test�Odatabase�W
	//useUnicode=true&characterEncoding=Big5�ϥΪ��s�X

	    }
	    catch(ClassNotFoundException e)
	    {
	      System.out.println("DriverClassNotFound :"+e.toString());
	    }//���i��|����sqlexception
	    catch(SQLException x) {
	      System.out.println("Exception :"+x.toString());
	    }

	  }

	  //�d�߸��
	  //�i�H�ݬݦ^�ǵ��G���Ψ��o��Ƥ覡
	  public List SelectRowValue(String selectSQL){
	    try
	    {
	      List rsList=new ArrayList();
	      stat = con.createStatement();
	      rs = stat.executeQuery(selectSQL);
	      while(rs.next()){
	    	  rsList.add(rs.getString("row"));
	      }
	      return rsList;
	    }
	    catch(SQLException e)
	    {
	      System.out.println("DropDB Exception :" + e.toString());
	    }
	    finally
	    {
	      Close();
	    }
	    return null;
	  }

	  public int SelectMax(String selectSQL){
	    try
	    {
	      stat = con.createStatement();
	      rs = stat.executeQuery(selectSQL);
	      //System.out.println(selectSQL);

	      while(rs.next()){
	      //System.out.println(rs.getInt("MAX(`row`)"));
	    	  //System.out.println(rs.findColumn("MAX('Row')"));
	    	  //Max = Integer.parseInt((String) rs.getObject("MAX('Row')"));
	    	  return rs.getInt("MAX(`Row`)");
	      }
	      /*
	      while(rs.next())
	      {
	        System.out.println(rs.getInt("accountid")+"\t\t"+
	            rs.getString("major"));
	      }
	      */
	    }
	    catch(SQLException e)
	    {
	      System.out.println("DropDB Exception :" + e.toString());
	    }
	    finally
	    {
	      Close();
	    }
	    return 0;
	  }


	  public List SelectRow(String selectSQL){
		  List<Integer> Resultlist = new ArrayList();
		    try
		    {
		      stat = con.createStatement();
		      rs = stat.executeQuery(selectSQL);
		      while(rs.next())
		      {
		    	  Resultlist.add(rs.getInt("Row"));
		      }
		      return Resultlist;


		    }
		    catch(SQLException e)
		    {
		      System.out.println("DropDB Exception :" + e.toString());
		    }
		    finally
		    {
		      Close();
		    }
			return null;

		  }



	  public int SelectMaxTab(String selectSQL){
		    try
		    {
		      stat = con.createStatement();
		      rs = stat.executeQuery(selectSQL);
		      //System.out.println(selectSQL);
		      while(rs.next()){
		      //System.out.println(rs.getInt("MAX(`row`)"));
		    	  return rs.getInt("MAX(`table`)");
		      }
		      /*
		      while(rs.next())
		      {
		        System.out.println(rs.getInt("accountid")+"\t\t"+
		            rs.getString("major"));
		      }
		      */
		    }
		    catch(SQLException e)
		    {
		      System.out.println("DropDB Exception :" + e.toString());
		    }
		    finally
		    {
		      Close();
		    }
		    return 0;
	  }

	  public void execUpdate(String exeUpdateSQL){
		  try{
			  stat = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
			  stat.executeUpdate(exeUpdateSQL);
		  }catch(SQLException e){
			  System.out.println("Close Exception :" + e.toString());
		  }
	  }

	  //����ϥΧ���Ʈw��,�O�o�n�����Ҧ�Object
	  //�_�h�b����Timeout��,�i��|��Connection poor�����p
	  private void Close()
	  {
	    try
	    {
	      if(rs!=null)
	      {
	        rs.close();
	        rs = null;
	      }
	      if(stat!=null)
	      {
	        stat.close();
	        stat = null;
	      }
	      if(pst!=null)
	      {
	        pst.close();
	        pst = null;
	      }
	    }
	    catch(SQLException e)
	    {
	      System.out.println("Close Exception :" + e.toString());
	    }
	  }

}
