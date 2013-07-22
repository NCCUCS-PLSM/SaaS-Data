
<?php

// No direct access to this file
defined('_JEXEC') or die;

JHtml::_('behavior.tooltip');
$document =& JFactory::getDocument();


$document->addStyleSheet('components/com_ola/assets/jqueryui/css/ui-lightness/jquery-ui-1.10.3.custom.min.css');

$document->addScript("components/com_ola/assets/jqueryui/js/jquery-1.9.1.js");
$document->addScript("components/com_ola/assets/jqueryui/js/jquery-ui-1.10.3.custom.min.js");


?>
<style>

#pList ul {width:900;list-style:none} 
#pList li {margin: 3px 3px 3px 0; padding: 1px;float:left;width:180px;text-align:center;list-style:none} 

div.block{
  overflow:hidden;
}
div.block label{
  width:160px;
  display:block;
  float:left;
  text-align:left;
}
div.block .input{
  margin-left:4px;
  float:left;
}
div.block select{
  width:45px;
  height:25px;
  display:block;
  float:left;
  text-align:left;
}
div.block .button{
  margin-left:4px;
  height:25px;
  float:left;
}

#container{width:100%;}
#left{float:left;width:100px;}
#right{float:right;width:150px;}
#center{margin:0 auto;width:100px;}


</style>
<script type="text/javascript">


jQuery.noConflict();
(function($) {
  $(function() {

	  var loadProduct = function (data){
			
			$("#pList").empty();
				//alert(data);
					$.each(data, function(i, item){
						
							var id = item.productId;
							var photoPath = "";
							var li =$('<li/>');
							//console.log(item.thumbnailImage);
							var tbImg = "http://"+ document.domain +item.thumbnailImage;
							var img=$('<img/>').attr('src',tbImg).attr('width','170').attr('height','148')
								.attr('class','transparentimg').attr('onmouseover','this.style.cursor="pointer";this.style.cursor="hand"')
								.attr('onmouseout','this.style.cursor="default"');
							
							$(li).append($(img));
						
						    $(li).append('<a href="#">'+item.productName+'<input type="hidden" id="prdId" name="prdId" value="'+ item.productId +'"/></a>');
							//$(li).append("<h3></h3>");
							$(li).append("<p><b></b>"+item.unitPrice+"</p>");
							

							$("#pList").append(li);
						
					});
				
		    return false;

		}
	  
	  var plUrl = "http://localhost:8080/shoppingmallrestful/plist?tid="+$("#tenantId").val();
	  var handleProduct = function () {
        jQuery.ajax({
            type: "GET",
     	   	  url: plUrl,
            dataType: 'jsonp',
            jsonpCallback: 'processData',
            success: function (data) {
                loadProduct(data)
            },
            error: function (jqXHR, exception) {
          	    if (jqXHR.status === 0) {
                      alert('Not connect.\n Verify Network.');
                  } else if (jqXHR.status == 404) {
                      alert('Requested page not found. [404]');
                  } else if (jqXHR.status == 500) {
                      alert('Internal Server Error [500].');
                  } else if (exception === 'parsererror') {
                      alert('Requested JSON parse failed.');
                  } else if (exception === 'timeout') {
                      alert('Time out error.');
                  } else if (exception === 'abort') {
                      alert('Ajax request aborted.');
                  } else {
                      alert('Uncaught Error.\n' + jqXHR.responseText);
                  }
            }
        });
        return false
    };

	 
	  var handleProductDtl = function (purl) {
        jQuery.ajax({
            type: "GET",
     	   	  url:purl ,
            dataType: 'jsonp',
            jsonpCallback: 'processData',
            success: function (data) {
	           	$("#pid").val(data.productId);
	              $("#pName").text(data.productName);
	              $("#pUp").text("Price: "+ data.unitPrice);
	              var img = "http://"+document.domain+data.thumbnailImage;
				$("#tbImg").attr("src" ,img );
				$("#desc").text(data.description);

				$("#divCf").empty();
          	  $.each(data.customFields, function(i, item){
					//console.log(item.fieldNum);
          		  if(item.fieldNum >= 4){
          			//console.log(item.type.value);
          			if(item.type.value ==0){
              			var p =$('<p><h3>'+ item.value+'</h3></>');
    					$("#divCf").append(p);
    	    		}else if(item.type.value==3 && item.value != null && item.value !=""){
    	    			var tbImg = "http://"+ document.domain +item.value;
						var img=$('<img/>').attr('src',tbImg).attr('width','170').attr('height','148')
							.attr('class','transparentimg').attr('onmouseover','this.style.cursor="pointer";this.style.cursor="hand"')
							.attr('onmouseout','this.style.cursor="default"');
						
						$("#divCf").append($(img));

        	    	}else if(item.type.value == 6 && item.value != null && item.value !=""){
					  // console.log(item.value);
        	    		var div = $('<div/>').html(item.value);	
        	    		$("#divCf").append(div);

            	   }
                 }
          		
          		
					
			  });
            },
            error: function (jqXHR, exception) {
          	    if (jqXHR.status === 0) {
                      alert('Not connect.\n Verify Network.');
                  } else if (jqXHR.status == 404) {
                      alert('Requested page not found. [404]');
                  } else if (jqXHR.status == 500) {
                      alert('Internal Server Error [500].');
                  } else if (exception === 'parsererror') {
                      alert('Requested JSON parse failed.');
                  } else if (exception === 'timeout') {
                      alert('Time out error.');
                  } else if (exception === 'abort') {
                      alert('Ajax request aborted.');
                  } else {
                      alert('Uncaught Error.\n' + jqXHR.responseText);
                  }
            }
        });
        return false
    };
    var pvs0 = function(){
    	var tUrl = "http://localhost:8080/shoppingmallrestful/t?tid="+$("#tenantId").val();
    	  $.when(jQuery.ajax({
			  type: "GET",
     	   	  url: tUrl,
            dataType: 'jsonp',
            jsonpCallback: 'processData',
            success: function (data) {
               $("#tName").val(data.tenantName);
            },
            error: function (jqXHR, exception) {
            }
		   })).done(function(a1){
		    	$("#pList").show();
		    	$("#pDtl").hide();
				$("#divCart").hide();
				$("#left").hide();
				$("#right").show();
			    handleProduct();
		   })
    }

    var pvs1 = function(purl){
    	$("#pVState").val(1);
       	$("#pDtl").show();
    	$("#pList").hide();
    	$("#divCart").hide();
    	$("#right").show();
    	$("#left").show();
    	handleProductDtl(purl);
    }

    var pvs2 = function(){
        //alert(112);
    	$("#pDtl").hide();
		$("#pList").hide();
		$("#divCart").show();
		$("#right").hide();
		$("#left").show();
    }
	  
	  if($("#pVState").val() ==0){
		  pvs0();
	  }
	

	  $('#pList').on('click','li a' ,function(event){
		  event.preventDefault();
		  	var h = $(this).find('input');
      		var purl = 'http://localhost:8080/shoppingmallrestful/rp?pid='+h.val()+'&tid='+$("#tenantId").val();
      		pvs1(purl);
        	
		  }) ;
	  

	  $("#tblISOList").on('click', 'input:checkbox', function (event) {
		  event.preventDefault();
          $("#slectedIso").val($(this).parent().find("input[name='isoId']").val());
      });

	  $('#sLink').click(function(event) { 
		 	event.preventDefault();
	       	pvs2();
	      
	  }); 

	  $('#plLink').click(function(event) { 
		  	event.preventDefault();
	       	pvs0();
	     
	  }); 
	  

	  $( "#addCart" ).button().click(function() {

		  var acUrl = 'http://localhost/joomla/index.php/?option=com_ola&task=ajax.addCart&format=raw';
			   $.when(jQuery.ajax({
			        type: "POST",
			 	   	  url:acUrl,
			 	   	data: { tid:$("#tenantId").val() , tname: $("#tName").val() ,pid: $("#pid").val(), pname: $("#pName").text() , qty: $("#selQty").val()  },
			        success: function () {
		                alert("1 item added to Cart");
			        },
			        error:function (jqXHR, exception) {
			          }
			    })).done(function(a1){
			    	var div =$('<div class="block"/>');
			    	$(div).append('<input type="hidden" id="tId" name="tId" value="'+ $("#tenantId").val() +'" >')
			    	$(div).append('<label>'+ $("#tName").val() +'</label>')
					$(div).append('<label>'+ $("#pName").text() +'</label>')
					$(div).append('<input type="hidden" id="prdId" name="prdId" value="'+ $("#pid").val() +'" >')
					$(div).append('<input class="input" type="text" id="qty" name="qty" value="'+ $("#selQty").val()+'"/>')
					$(div).appendTo($("#cart"));
					$("#cart").append("<p></p>");
					$("#selQty").val(1);
					
				})

	
		});

	  $( "#checkOut" ).button().click(function() {
			var jsonVal = "{\"CustomerId\":"+ $("#userId").val() +",\"Customer\":\""+ $("#userName").val() +"\" , \"OrderLineitem\" :";
			jsonVal += doOrderItem() + "}";

					$("#cart").empty();
					var csUrl = 'http://localhost/joomla/index.php/?option=com_ola&task=ajax.clearSession&format=raw';
					jQuery.ajax({
				        type: "GET",
				 	   	  url:csUrl,
				        success: function () {
				        	crossDomainPost("http://localhost:8080/shoppingmallrestful/order",jsonVal);
				        	pvs0();
				        },
				        error:function (jqXHR, exception) {
				    	    if (jqXHR.status === 0) {
				                alert('Not connect.\n Verify Network.');
				            } else if (jqXHR.status == 404) {
				                alert('Requested page not found. [404]');
				            } else if (jqXHR.status == 500) {
				                alert('Internal Server Error [500].');
				            } else if (exception === 'parsererror') {
				                alert('Requested JSON parse failed.');
				            } else if (exception === 'timeout') {
				                alert('Time out error.');
				            } else if (exception === 'abort') {
				                alert('Ajax request aborted.');
				            } else {
				                alert('Uncaught Error.\n' + jqXHR.responseText);
				            }
				          }
				    })

			  })
			  
	 $( "#cancel" ).button().click(function() {
			
					var csUrl = 'http://localhost/joomla/index.php/?option=com_ola&task=ajax.clearSession&format=raw';
					jQuery.ajax({
				        type: "GET",
				 	   	  url:csUrl,
				        success: function () {
				        	$("#cart").empty();
				        	pvs0();
				        },
				        error:function (jqXHR, exception) {
				    	    if (jqXHR.status === 0) {
				                alert('Not connect.\n Verify Network.');
				            } else if (jqXHR.status == 404) {
				                alert('Requested page not found. [404]');
				            } else if (jqXHR.status == 500) {
				                alert('Internal Server Error [500].');
				            } else if (exception === 'parsererror') {
				                alert('Requested JSON parse failed.');
				            } else if (exception === 'timeout') {
				                alert('Time out error.');
				            } else if (exception === 'abort') {
				                alert('Ajax request aborted.');
				            } else {
				                alert('Uncaught Error.\n' + jqXHR.responseText);
				            }
				          }
				    })

			  });		  
	

	  function doOrderItem(){
			var jsonVal= "[";
			 $("#cart").find("div").each(function() {
				 jsonVal +="{\"tenantId\":"+ $(this).find("input[name='tId']").val() +",\"productId\":\""+  $(this).find("input[name='prdId']").val() +"\",\"qty\":\""+  $(this).find("input[name='qty']").val()+"\"},";
				});
			 jsonVal = jsonVal.substring(0,jsonVal.length-1);
			 jsonVal +="]";

			return jsonVal;
			
		}
	  
	  function crossDomainPost(url, text) {
		    // Add the iframe with a unique name
		    var iframe = document.createElement("iframe");
		    var uniqueNameOfFrame = "sum";
		    document.body.appendChild(iframe);
		    iframe.style.display = "none";
		    iframe.contentWindow.name = uniqueNameOfFrame;
		
		    // construct a form with hidden inputs, targeting the iframe
		    var form = document.createElement("form");
		    form.target = uniqueNameOfFrame;
		    form.action = url;
		    form.method = "POST";
		
		    // repeat for each parameter
		    var input = document.createElement("input");
		    input.type = "hidden";
		    input.name = "text";
		    input.value = text;
		    form.appendChild(input);
		    document.body.appendChild(form);

	    	form.submit();
		}
	  

  });
})(jQuery);

</script>
<?php  $user =& JFactory::getUser();
$uId = 0; $uName = "";
if($user != null){
	$uId = $user->id;
	$uName = $user->name;
}
?>

<input type="hidden" id="tName" name="tName" value="" >
<input type="hidden" id="userId" name="userId" value="<?php echo $uId ;?>" >
<input type="hidden" id="userName" name="userName" value="<?php echo $uName ;?>" >
 <input type="hidden" id="tenantId" name="tenantId" value="<?php echo $this->item; ?>" >
 <input type="hidden" id="pid" name="pid" value="<?php echo $this->pid; ?>" >
 <input type="hidden" id="pVState" name="pVState" value="0" >

 <p></p>
<div id="container" style="">
  <div id="left"><h1><a id="plLink">Products</a></h1></div>
  <div id="right"><h1><a id="sLink">Shopping Cart</a></h1></div>
  <div id="center"></div>
</div>

<p></p>
<p></p>
<ul id="pList"  > 
</ul>

<p></p>
<p></p>

<table id="pDtl" style="display: none">
<tr>
<td>
<div >
<h1 id="pName"></h1>
<h2 id="pUp"></h2>
<div class="block">
<select id="selQty">
<option value="1">1</option>
<option value="2">2</option>
<option value="3">3</option>
<option value="4">4</option>
<option value="5">5</option>
<option value="6">6</option>
<option value="7">7</option>
<option value="8">8</option>
<option value="9">9</option>
</select>
&nbsp;&nbsp;
<button id="addCart">Add to Cart</button>
<p>
<h2><div id="desc"></div></h2>
<br /><br />
<span class="color01"></span></p>
    	 
<img id="tbImg" width="600" height="500" src="" border="0">
</div>
</div>
</td>
</tr>
<tr><td>
<div id="divCf"></div>
</td>
</tr>
</table>

<p></p>
<table  >
<tr><td>

<div  id="divCart" style="width: 895px;display: none">

	<h1 class=""><?php echo $uName ;?>'s Shopping Cart</h1>
	<div class="ui-widget-content">
	

<!-- use userid to check wether need to clear cart -->
		<ol id="cart">
		<?php 
			$session = JFactory::getSession();
			$uniqueName = $user->id."cart";
			$cart = $session->get('cart', array() ,$uniqueName);
			if($cart != null){
			foreach ($cart['items'] as $value) {
			?>
			<div class="block">
			<input type="hidden" id="tId" name="tId" value="<?php echo $value['tid']; ?>" >
			<label><?php echo $value['tname']; ?></label>
			<label><?php echo $value['pname']; ?></label>
				<input type="hidden" id="prdId" name="prdId" value="<?php echo $value['pid']; ?>" >
				<input class="input" type="text" id="qty" name="qty" value="<?php 	echo $value['qty']; ?>"/>
			<p></p>
		</div>
<?php }
}?>

</ol>
	</div>
	<p></p>
	<p></p>
<button id="checkOut">Check out</button>
<button id="cancel">Remove Shopping Cart</button>
</div>
</td>
</tr>
</table>





