
<%@ page session="false" %>
<html>
<head>
	<title>Home</title>


	<link rel="stylesheet" type="text/css" media="screen" href="<c:url value="/resources/jqueryui/css/redmond/jquery-ui-1.8.16.custom.css" />" />
	<script type="text/javascript" src="<c:url value="/resources/jquery/jquery-1.7.1.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/jqueryui/js/jquery-ui-1.8.16.custom.min.js" />"></script>
 	<script type="text/javascript" src="<c:url value="/resources/jqueryui/js/jquery.layout-1.2.0.js" />"></script>
 	
 <style>
html, body {
	margin: 0;			/* Remove body margin/padding */
	padding: 0;
	overflow: hidden;	/* Remove scroll bars on browser window */	
    font-size: 85%;
}

	#vendorList .ui-selecting { background: #FECA40; }
	#vendorList .ui-selected { background: #F39814; color: white; }
	#vendorList { list-style-type: none; margin: 0; padding: 0; }
	#vendorList li { margin: 3px; padding: 1px; float: left; width: 100px; height: 20px;  }


</style>
 
 
	<script type="text/javascript">
	
	/*
	$(function() {
		
		
		   var handleSvg = function () {
               $.ajax({
                   type: "GET",
            	   url: 'http://localhost:8080/shoppingmallrestful/olist',
                   cache: false,
                   success: function (data) {
                       alert(data);
                   },
                   error: function (jqXHR, exception) {
                      alert(1123);
                   }
               });
               return false
           };
           handleSvg();
		
           
		$.ajax({
			   type: "GET",
			   url: "http://localhost:8080/shoppingmallrestful/olist",
			   dataType: "jsonp" ,
			   success: function (data) {
				   alert(111);
				   $.each(data, function(i, item){
						alert(i);
						//jQuery("#list4").jqGrid('addRowData',i+1,item);
					});
			   }
			});
		
		$( "#catalog" ).accordion();
		$( "#catalog li" ).draggable({
			appendTo: "body",
			helper: "clone"
		});
		$( "#cart ol" ).droppable({
			activeClass: "ui-state-default",
			hoverClass: "ui-state-hover",
			accept: ":not(.ui-sortable-helper)",
			drop: function( event, ui ) {
				$( this ).find( ".placeholder" ).remove();
				$( "<li></li>" ).text( ui.draggable.text() ).val(ui.draggable.val()).appendTo( this );
				$( "<label>Qty</label><input id=\"p"+ui.draggable.val() +"\" type='text'></input>" ).appendTo( this );
			}
		}).sortable({
			items: "li:not(.placeholder)",
			sort: function() {
				// gets added unintentionally by droppable interacting with sortable
				// using connectWithSortable fixes this, but doesn't allow you to customize active/hoverClass options
				$( this ).removeClass( "ui-state-default" );
			}
		});
		
		  $('body').layout({ applyDefaultStyles: true });
		  
		  
		  initVendor();
		  
			$( "#vendorList" ).selectable({
				stop: function() {

					$( ".ui-selected", this ).each(function() {
		
						listProducts($(this).val());
						
					});
					
				
				}
			});
			
			
			$("#order").submit(function() {
				
				var jsonVal = "{ \"Vendor\":\""+ $("#hidV").val()+"\" ,\"Customer\":\""+ $("#customer").val() +"\" , \"OrderLineitem\" :";
				jsonVal += doOrderItem() + "}";
				$("#hid1").val(jsonVal);
				//alert(jsonVal);
				//return false;
				
			});
		  
		  
	});
	
	function doOrderItem(){
		var jsonVal= "[";
		
		 $("#cart").find("li").each(function() {
			 var id = "p" + $(this).val();
			 jsonVal +="{\"productId\":\""+ $(this).val()+"\",\"qty\":\""+ $("#" + id).val()+"\"},";
			});
		 jsonVal = jsonVal.substring(0,jsonVal.length-1);
		 jsonVal +="]";

		return jsonVal;
		
	}
	
	
	function initVendor(){
	
		var ul = $("#vendorList");
		ul.empty();
		
		$.getJSON("/abc/vlist", function(data) {
			$.each(data, function(i, item){
				
				$( "<li></li>" ).text(item.vendorName ).val(item.vendorId).appendTo(ul);
				
			});
		});
		
		$("#vendors").accordion();
			
	};
	
	
	function listProducts(arg){
		$("#hidV").val(arg);
		var ul = $("#productList");
		ul.empty();
		
		$.getJSON("/abc/plist/?vendorId="+ arg, function(data) {
			$.each(data, function(i, item){
				
				$( "<li></li>" ).text(item.productName ).val(item.productId).appendTo(ul).draggable({
					appendTo: "body",
					helper: "clone"
				});
				
			});
		});
		
		$("#catalog").accordion();
		
	};
	
	*/
	

	</script>
	
	

	
</head>
<body>



<div id="LeftPane" class="ui-layout-west ui-widget ui-widget-content">
	


<div id="VendorDiv" >
	<h1 class="ui-widget-header">Vendors</h1>	
	<div id="vendors" >
		<h3><a href="#">Vendor</a></h3>
		<div>
			<ul id="vendorList" style="height:500px">

			</ul>
		</div>
		
	</div>
</div>
<input type="hidden" id="hidV" name="hidV" value="" />

</div> <!-- #LeftPane -->


	<div id="centerPane" class="ui-layout-center ui-helper-reset ui-widget-content" >
	<div id="switcher"></div>
<div class="demo">
	
<div id="products">
	<h1 class="ui-widget-header">Products</h1>	
	<div id="catalog">
		<h3><a href="#">Product</a></h3>
		<div>
			<ul id="productList" style="height:400px">
			
			</ul>
		</div>
	
	</div>
</div>

<div id="cart">
	<h1 class="ui-widget-header">Shopping Cart</h1>
	<div class="ui-widget-content">
	<label>Customer</label>
	<input id="customer" type="text"/>
		<ol id="cart">
			<li class="placeholder">Add your items here</li>
		</ol>
	</div>
</div>


<form id="order" action="order" method="post">
<div>
<input type="hidden" id="hid1" name="hid1" value="" />
  <input type="submit" name="" value="Yes" />
</div>
</form>


</div><!-- End demo -->
</div>



</body>
</html>
