<?php

/**
 * @package		Joomla.Tutorials
 * @subpackage	Component
 * @copyright	Copyright (C) 2005 - 2010 Open Source Matters, Inc. All rights reserved.
 * @license		License GNU General Public License version 2 or later; see LICENSE.txt
 */

// No direct access to this file
defined('_JEXEC') or die;

JHtml::_('behavior.tooltip');
$document =& JFactory::getDocument();

$document->addStyleSheet('../components/com_ola/assets/jqueryui/css/ui-lightness/jquery-ui-1.10.3.custom.min.css');
$document->addStyleSheet('../components/com_ola/assets/jqGrid/css/ui.jqgrid.css');

$document->addScript("../components/com_ola/assets/jqueryui/js/jquery-1.9.1.js");
$document->addScript("../components/com_ola/assets/jqueryui/js/jquery-ui-1.10.3.custom.min.js");
$document->addScript("../components/com_ola/assets/jqGrid/js/i18n/grid.locale-en.js");
$document->addScript("../components/com_ola/assets/jqGrid/js/jquery.jqGrid.min.js");

?>
<style>
    #dialog label, #dialog input { display:block; }
  #dialog label { margin-top: 0.5em; }
  #dialog input, #dialog textarea { width: 95%; }
  .no-close .ui-dialog-titlebar-close {display: none }
.ui-jqgrid .ui-jqgrid-bdiv {
  position: relative; 
  margin: 0em; 
  padding:0; 
  /*overflow: auto;*/ 
  overflow-x:hidden; 
  overflow-y:auto; 
  text-align:left;
}
</style>
<script type="text/javascript">

jQuery.noConflict();
(function($) {
  $(function() {

	  var userUrl =  'http://localhost:8080/shoppingmallrestful/u?uid='+ $("#userId").val();  
      $.when(jQuery.ajax({
          type: "GET",
   	   	  url:userUrl,
       	  cache: false,
          dataType: 'jsonp',
          jsonpCallback: 'processData',
          success: function (data) {
              $("#tenantId").val(data.tenantId);
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
      })).done(function(a1){
          
          var olistUrl2 = 'http://localhost:8080/shoppingmallrestful/olist?tid='+ $("#tenantId").val();
          jQuery("#list4").jqGrid({
        	   url: olistUrl2,
               datatype: "jsonp",
               jsonReader : {
             	     repeatitems: false
             	   },
    			height: 350,
    		   	colNames:['Order Id','Customer', 'Order Date' , "Amount"],
    		   	colModel:[
    		   		{name:'orderId',index:'orderId', width:160},
    		   		{name:'customer',index:'customer', width:150},
    		   		{name:'orderDate',index:'orderDate', width:150},
    		   		{name:'orderAmount',index:'orderAmount', width:80}
    		   	],
    		   	caption: "Order",
    		   	ondblClickRow:function(id){
    			    	 var rowData = jQuery("#list4").jqGrid('getRowData', id );		    	 
    			    	 openForm(rowData['orderId']);
    			     }

    		});
    	})
    
      
    	jQuery('#dialog-form').dialog({
			autoOpen: false,
			dialogClass: 'no-close',
			height: 500,
			width: 500,
			modal: false,
			buttons: {
				"OK": function() {
					$( this ).dialog( "close" );
				},
				"Cancel": function() {
					$( this ).dialog( "close" );
				},

			},
		});
		
		
		
		
	function openForm(arg) {
		
		$( "#dialog-form" ).dialog( "open" );
		//jQuery("#list5").empty();
		//jQuery("#list5").jqGrid('clearGridData');
		var odurl = 'http://localhost:8080/shoppingmallrestful/od?tid='+ $("#tenantId").val()+'&oid='+arg;
		jQuery("#list5").jqGrid({
			  url: odurl,
            datatype: "jsonp",
            jsonReader : {
          	     repeatitems: false
          	   },
		   	colNames:['Item Id','Prodcut Name', 'UnitPrice','Qty','SubTotal'],
		   	colModel:[
		   		{name:'orderLineitemId',index:'orderLineitemId', width:120},
		   		{name:'productName',index:'productName', width:140},
		   		{name:'unitPrice',index:'unitPrice', width:100},
		   		{name:'qty',index:'qty', width:50},
		   		{name:'subTotal',index:'subTotal', width:60}
		   	],
		   	height: 350,
		   	caption: "Order Lineitem"

		});
		
	}
        
  });
})(jQuery);




</script>

<?php  $user =& JFactory::getUser();?>
<input type="hidden" id="userId" name="userId" value="<?php echo $user->id;?>" >
<input type="hidden" id="tenantId" name="tenantId" value="0" >
<!-- 
<form action="<?php// echo JRoute::_('index.php?option=com_store'); ?>" method="post" name="adminForm">
	<table class="adminlist">
		<thead><?php //echo $this->loadTemplate('head');?></thead>
		<tfoot><?php //echo $this->loadTemplate('foot');?></tfoot>
		<tbody><?php //echo $this->loadTemplate('body');?></tbody>
	</table>
	<div>
		<input type="hidden" name="task" value="" />
		<input type="hidden" name="boxchecked" value="0" />
		<?php //echo JHtml::_('form.token'); ?>
	</div>
</form>
-->



<div>

<div  >
<table id="list4"></table>
</div>



<div id="dialog-form" title="">
	<table id="list5"></table>
</div>
</div>
