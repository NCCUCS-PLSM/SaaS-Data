<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title></title>

<link rel="stylesheet" type="text/css" media="screen" href="<c:url value="/resources/jqueryui/css/redmond/jquery-ui-1.8.16.custom.css" />" />
	<link rel="stylesheet" type="text/css" media="screen" href="<c:url value="/resources/jgGrid/css/ui.jqgrid.css" />" />
		<link rel="stylesheet" type="text/css" media="screen" href="<c:url value="/resources/jqueryui/css/cTab.css" />" />
		
<script type="text/javascript" src="<c:url value="/resources/jquery/jquery-1.7.1.js" />"></script>
<script type="text/javascript" src="<c:url value="/resources/jgGrid/js/i18n/grid.locale-en.js" />"></script>
<script type="text/javascript" src="<c:url value="/resources/jgGrid/js/jquery.jqGrid.src.js" />"></script>
<script type="text/javascript" src="<c:url value="/resources/jqueryui/js/jquery-ui-1.8.16.custom.min.js" />"></script>


	<script type="text/javascript">
	
	$(function(){
			
		
		jQuery("#list4").jqGrid({
			datatype: "local",
			width:400,
			height: 350,
		   	colNames:['Order Id','Customer', 'Order Date' , "Amount"],
		   	colModel:[
		   		{name:'orderId',index:'orderId', width:60, sorttype:"int"},
		   		{name:'customer',index:'customer', width:100},
		   		{name:'orderDate',index:'orderDate', width:80, align:"right",sorttype:"float"},
		   		{name:'orderAmount',index:'orderAmount', width:80, align:"right",sorttype:"float",}
		   	],
		   	caption: "Order",

		   	ondblClickRow:function(id){
			    	 var rowData = jQuery("#list4").jqGrid('getRowData', id );		    	 
			    	 openForm(rowData['orderId']);
			     }

		});

		
		$.getJSON("/ShoppingForceWeb/backend/order/olist", function(data) {
			$.each(data, function(i, item){
				jQuery("#list4").jqGrid('addRowData',i+1,item);
			});
			
			
		});
		
		$( "#dialog-form" ).dialog({
			autoOpen: false,
			height: 600,
			width: 750,
			modal: true,
			buttons: {
				"確定": function() {
					$( this ).dialog( "close" );
				},
				"取消": function() {
					$( this ).dialog( "close" );
				},

			},
		});
		
		jQuery("#list5").jqGrid({
			datatype: "local",
			width:700,
			height: 350,
		   	colNames:['Item Id','Prodcut Name', 'UnitPrice','Qty','SubTotal'],
		   	colModel:[
		   		{name:'orderLineitemId',index:'orderLineitemId', width:60, sorttype:"int"},
		   		{name:'productName',index:'productName', width:140},
		   		{name:'unitPrice',index:'unitPrice', width:100, align:"right",sorttype:"float",},
		   		{name:'qty',index:'qty', width:80, align:"right",sorttype:"float",},
		   		{name:'subTotal',index:'subTotal', width:80, align:"right",sorttype:"float",}
		   	],
		   	caption: "Order Lineitem"

		});
		
	
	});
	
	function openForm(arg) {
		
		$( "#dialog-form" ).dialog( "open" );
		jQuery("#list5").empty();
		jQuery("#list5").jqGrid('clearGridData');
		$.getJSON("/ShoppingForceWeb/backend/order/oilist?oid="+arg, function(data) {
			$.each(data, function(i, item){
				jQuery("#list5").jqGrid('addRowData',i+1,item);
			});
			
			
		});
		
		
	
	}
	
	</script>

</head>
<body>

 <div>
        <ul id="tab-container-1-nav" style="width: 99%">
            <li style="background-color: lightgrey"><a class="" href="product">Product</a></li>
            <li style="background-color: lightgrey"><a class="active" href="order">Order</a></li>
        </ul>

    </div>
    
    <div>
	<P>Hello ${userName}.  <a href="../j_spring_security_logout">Logout</a></P>
    
    </div>
    
<div  id="tabcontent" >

<span id="toolbar" class="ui-widget-header ui-corner-all">
<p></p>
<p></p>
<p></p>
</span>

<div>
<table id="list4"></table>

</div>



<div id="dialog-form" title="">
	<table id="list5"></table>
</div>
</div>

</body>
</html>