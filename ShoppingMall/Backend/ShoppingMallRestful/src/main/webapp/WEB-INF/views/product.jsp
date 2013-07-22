
<%@ page session="false" language="java" contentType="text/html"
	pageEncoding="utf-8"%>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Product</title>
	
	<link rel="stylesheet" type="text/css" media="screen" href="<c:url value="/resources/jqueryui/css/redmond/jquery-ui-1.8.16.custom.css" />" />
	<link rel="stylesheet" type="text/css" media="screen" href="<c:url value="/resources/jgGrid/css/ui.jqgrid.css" />" />
	<link rel="stylesheet" type="text/css" media="screen" href="<c:url value="/resources/jqueryui/css/cTab.css" />" />
 
<script type="text/javascript" src="<c:url value="/resources/jquery/jquery-1.7.1.js" />"></script>
<script type="text/javascript" src="<c:url value="/resources/jgGrid/js/i18n/grid.locale-en.js" />"></script>
<script type="text/javascript" src="<c:url value="/resources/jgGrid/js/jquery.jqGrid.src.js" />"></script>
<script type="text/javascript" src="<c:url value="/resources/jqueryui/js/jquery-ui-1.8.16.custom.min.js" />"></script>

<style>
	#toolbar {
		padding: 10px 4px;
		font-size: 10px;
	}
	</style>
	
	<script type="text/javascript">
	
	$(function(){
		$("button").button();
	
		jQuery("#list4").jqGrid({
			datatype: "local",
			width:400,
			height: 350,
		   	colNames:['ProductId','ProductName', 'UnitPrice'],
		   	colModel:[
		   		{name:'productId',index:'productId', width:60, sorttype:"int"},
		   		{name:'productName',index:'productName', width:100},
		   		{name:'unitPrice',index:'unitPrice', width:80, align:"right",sorttype:"float"}
		   	],
		   	caption: "Product",

		     ondblClickRow:function(id){
		    	 var rowData = jQuery("#list4").jqGrid('getRowData', id );		    	 
		    	 openForm(rowData['productId']);
		     }

		});

		
		$.getJSON("/ShoppingForceWeb/backend/product/pslist", function(data) {
			$.each(data, function(i, item){
				jQuery("#list4").jqGrid('addRowData',i+1,item);
			});
		});
		
		
		$( "#dialog-form" ).dialog({
			autoOpen: false,
			height: 500,
			width: 600,
			modal: true,
			buttons: {
				"確定": function() {
					saveProduct();
					$( this ).dialog( "close" );
				},
				"取消": function() {
					$( this ).dialog( "close" );
				},

			},
		});
		
		$( "#dialog-form2" ).dialog({
			autoOpen: false,
			height: 500,
			width: 500,
			modal: true,
			buttons: {
				"確定": function() {
					saveSchema();
					$( this ).dialog( "close" );
				},
				"取消": function() {
					$( this ).dialog( "close" );
				},

			},
		});
		
		$( "#pAdd" ).button().click(function() {
			addNewProduct();
		});
		
		$( "#fAdd" ).button().click(function() {
			addField();
		});
		
		$( "#pEdit" ).button().click(function() {
			openSchmeaForm();
		});
		
	
	});
	
	
	
	</script>
	
</head>
<body>

<script type="text/javascript">
function openForm(arg) {
	
	var pv = $("#productView");
	pv.empty();
	
	$.getJSON("/ShoppingForceWeb/backend/product/rpv?productId="+arg, function(data) {
		$.each(data.fields, function(i, item){				
			pv.append($(doProductView(item)));
		});
	});
	
	$( "#dialog-form" ).dialog( "open" );

}

function doProductView(item){
	
	var value ="";
	if(item.fieldValue != null) value= item.fieldValue;
	var appendStr = "<tr>";
	appendStr += "<td><label name=\"fieldName\">"+item.fieldName+"</label>";
	appendStr += "<input type=\"hidden\" name=\"controlType\" value=\""+ item.controlType +"\"/></td>";
	
	if(item.controlType =="label")
		appendStr += "<td><label name=\"fieldValue\">"+value+"</label></td>";
	
	if(item.controlType =="textbox")
		appendStr += "<td><input type=\"text\" name=\"fieldValue\" value=\""+value +"\" class=\"text ui-widget-content ui-corner-all\" /></td>";
		
	if(item.controlType =="image"){	
		appendStr += "<td><input type=\"file\" name=\"file\" class=\"text ui-widget-content ui-corner-all\" />";
		if(value!=null){
			appendStr +="<img src=\"http://localhost:8080/ShoppingForceWeb"+ value+"\" alt=\"\" width=\"304\" height=\"228\" />";	
		}
		appendStr +="</td>";
	}
			
	appendStr += "</tr>";
	
	return appendStr;
}

function addNewProduct(){
	
	var pv = $("#productView");
	pv.empty();
	$.getJSON("/ShoppingForceWeb/backend/product/sch", function(data) {
		$.each(data.fields, function(i, item){		
			pv.append($(doProductView(item)));
			});
		});
	
	$( "#dialog-form" ).dialog( "open" );
}

function openSchmeaForm() {
	var pv = $("#productSchema");
	pv.empty();
	$("<thead><tr><th>Field Name</th><th>Field Type</th></tr></thead><tbody>").appendTo(pv);
	
	$.getJSON("/ShoppingForceWeb/backend/product/sch", function(data) {
		$.each(data.fields, function(i, item){				
			var appendStr = "<tr>";
			appendStr += "<td><input type='textbox' name='fieldName' value='"+item.fieldName+"'></label></td>";
			appendStr += "<td><Select name='fieldType'>";

			if(item.controlType == "label"){
				appendStr += "<option selected>label</option>";
			}else{
				appendStr += "<option>label</option>";
			}
			
			if(item.controlType == "textbox"){
				appendStr += "<option selected>textbox</option>";
			}else{
				appendStr += "<option>textbox</option>";
			}
			
			if(item.controlType == "image"){
				appendStr += "<option selected>image</option>";
			}else{
				appendStr += "<option>image</option>";
			}
			
			appendStr += "</Select></td></tr>";
			pv.append($(appendStr));
		});
	});
	
	pv.append("</tbody>");
	
	$( "#dialog-form2" ).dialog( "open" );

}

function addField() {
	var pv = $("#productSchema");	
	var appendStr = "<tr>";
	appendStr += "<td><input name='fieldName' type='textbox'></input></td>";
	appendStr += "<td><Select name='fieldType'>";
	appendStr += "<option>label</option>";
	appendStr += "<option>textbox</option>";
	appendStr += "<option>image</option>";
	appendStr += "</Select><input name='isAdd' type='hidden' value='1'></td></tr>";
	pv.append($(appendStr));

}

function saveProduct(){

	var json = "{\"fields\":[";
	$("#productView").find("tr").each(function() {
		var fieldName = $(this).find('label[name="fieldName"]').text();
		var controlType = $(this).find('input[name="controlType"]').val();
		var fieldValue;
		if(controlType =="label"){
			fieldValue = $(this).find('label[name="fieldValue"]').text();
		}else{
			fieldValue = $(this).find('input[name="fieldValue"]').val();
		}
		
		
		json +="{\"fieldName\":\""+fieldName+"\",\"fieldValue\":\""+fieldValue+"\",\"controlType\":\""+controlType+"\"},";
	});
	
	json = json.substring(0,json.length-1);
	json +="]}";
	//alert(json);
	$("#hidProduct").val(json);
	$( "#dialog-form" ).dialog( "close" );
	$("#frmProduct").submit();

}

function saveSchema(){
	
	var json = "{\"fields\":[";
	$("#productSchema").find("tbody").find("tr").each(function() {
		var fieldName = $(this).find('input[name="fieldName"]').val();
		var fieldType = $(this).find('select[name="fieldType"]').find(":selected").text();
		var isAdd = "0";
		if( $(this).find('input[name="isAdd"]').val()!=null) 
			isAdd = $(this).find('input[name="isAdd"]').val();
		
		json +="{\"fieldName\":\""+fieldName+"\",\"controlType\":\""+fieldType+"\",\"isAdd\":\""+ isAdd +"\" },";
	});
	
	json = json.substring(0,json.length-1);
	json +="]}";

	$("#hidSchema").val(json);
	$( "#dialog-form2" ).dialog( "close" );
	$("#frmSchema").submit();
	
}

</script>
    <div>
        <ul id="tab-container-1-nav" style="width: 99%">
            <li style="background-color: lightgrey"><a class="active" href="product">Product</a></li>
            <li style="background-color: lightgrey"><a class="" href="order">Order</a></li>
        </ul>
    </div>
    <div>
	<P>Hello ${userName}.   <a href="../j_spring_security_logout">Logout</a></P>
    </div>
    
<div  id="tabcontent" >

<span id="toolbar" class="ui-widget-header ui-corner-all">
	<button id="pAdd">Add New Product</button>
	<button id="pEdit">Edit Product Schema</button>	
</span>

<p></p>

</div>
<table id="list4"></table>

<div id="dialog-form" title="">
<form id="frmProduct" action="../backend/product/save" method="post" enctype="multipart/form-data">
<div>
<table id="productView" class="ui-widget ui-widget-content">
</table>
<input type="hidden" id="hidProduct" name="hidProduct" value="" />
</div>
</form>
</div>

<div id="dialog-form2" title="">
<button id="fAdd" style="font-size: 10px">Add Field</button>
<table id="productSchema" class="ui-widget ui-widget-content">

  
</table>
</div>
<form id="frmSchema" action="../backend/product/schema" method="post">
<div>
<input type="hidden" id="hidSchema" name="hidSchema" value="" />
</div>
</form>



</body>
</html>
