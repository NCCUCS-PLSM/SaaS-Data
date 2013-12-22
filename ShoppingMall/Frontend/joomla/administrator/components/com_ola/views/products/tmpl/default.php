<?php

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

$document->addScript("../media/editors/tinymce/jscripts/tiny_mce/tiny_mce.js");
$document->addScriptDeclaration('
tinyMCE.init({
	// General
	directionality: "ltr",
	editor_selector : "mce_editable",
	language : "en",
	mode : "specific_textareas",
	skin : "default",
	theme : "advanced",
	// Cleanup/Output
	inline_styles : true,
	gecko_spellcheck : true,
	entity_encoding : "raw",
	extended_valid_elements : "hr[id|title|alt|class|width|size|noshade|style],img[class|src|alt|title|hspace|vspace|width|height|align|onmouseover|onmouseout|name|style],a[id|class|name|href|hreflang|target|title|onclick|rel|style]",
	force_br_newlines : false, force_p_newlines : true, forced_root_block : "p",
	invalid_elements : "script,applet,iframe",
	// URL
	relative_urls : false,
	remove_script_host : false,
	document_base_url : "http://localhost/joomla/",
	// Layout
	content_css : "http://localhost/joomla/templates/system/css/editor.css",
	// Advanced theme
	theme_advanced_toolbar_location : "top",
	theme_advanced_toolbar_align : "left",
	theme_advanced_source_editor_height : "550",
	theme_advanced_source_editor_width : "750",
	theme_advanced_resizing : true,
	theme_advanced_resize_horizontal : false,
	theme_advanced_statusbar_location : "bottom", theme_advanced_path : true
});');

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
              $("#hTname").text(data.tenantName);
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
    	  var olistUrl = 'http://localhost:8080/shoppingmallrestful/plist?tid='+ $("#tenantId").val();
          jQuery("#list4").jqGrid({
       	   url: olistUrl,
              datatype: "jsonp",
              jsonReader : {
           	     repeatitems: false
           	   },
   			height: 350,
   			colNames:['','objectId' ,'ProductId','ProductName', 'UnitPrice'],
  		   	colModel:[
			{name:'delete',  formatter: function (cellvalue, options, rowObject) {
				 $("#objId").val(rowObject["objectId"]);
			    return '<a href="#" rel="'+rowObject["objectId"] +"/"+ rowObject["productId"] + '">delete</a>';
			},width:50},
				{name:'objectId',index:'objectId', hidden:true},
  		   		{name:'productId',index:'productId', width:100},
  		   		{name:'productName',index:'productName', width:200},
  		   		{name:'unitPrice',index:'unitPrice', width:180}
  		   	],
  		   	caption: "Product",
   		   	ondblClickRow:function(id){
   		  	 var rowData = jQuery("#list4").jqGrid('getRowData', id );		    	 
		    	 openForm(rowData['productId']);
   			     }

   			});
    	});

      $("#list4").on("click","a",function(event){
    	   event.preventDefault();
    	   var ary = $(this).attr("rel").split("/");
    	   var json = '{"tid":'+ $("#tenantId").val() +',"customObjectId":'+ary[0]+',"coPkVal":"'+ary[1]+'"}';

	    	//console.log(json);
	    	crossDomainPost("http://localhost:8080/shoppingmallrestful/deldata",json,true);
    	   
       })
       

       var dialogRel = $( "#dialogRel" ).dialog({
        autoOpen: false,
        dialogClass: 'no-close',
        modal: false,
        width: 500,
        buttons: {
          OK: function() {
            $( this ).dialog( "close" );
            	
          }
        },
        close: function() {
        }
      });

      $( "#dialog-form" ).dialog({
			autoOpen: false,
			 dialogClass: 'no-close',
			height: 400,
			width: 450,
			modal: false,
			buttons: {
				"OK": function() {
					saveProduct();
					$( this ).dialog( "close" );
				},
				"Cancel": function() {
					$( this ).dialog( "close" );
				},

			},
		});
		
		$( "#dialog-form2" ).dialog({
			autoOpen: false,
			 dialogClass: 'no-close',
			height: 500,
			width: 500,
			modal: false,
			buttons: {
				"OK": function() {
					saveSchema();
					$( this ).dialog( "close" );
				},
				"Cancel": function() {
					$( this ).dialog( "close" );
				},

			},
		});


	       var dialogRel2 = $( "#relation" ).dialog({
	           autoOpen: false,
	           dialogClass: 'no-close',
	           modal: false,
	           width: 500,
	           buttons: {
	             "OK": function() {
	            	saveRel();
	               $( this ).dialog( "close" );
	               	
	             },
	       	"Cancel": function() {
				$( this ).dialog( "close" );
			}},
	         });

			
		$( "#pAdd" ).button().click(function() {
			openForm(0);
		});
		
		$( "#fAdd" ).button().click(function() {
			addField();
		});
		
		$( "#pEdit" ).button().click(function() {
			openSchmeaForm();
		});
		
		//data
		 var relAry;
		function openForm(arg) {
			$("#coPkVal").val(arg);
			var pv = $("#productView");
			pv.empty();
			$("<thead><tr><th>Name</th><th>Value</th></tr></thead><tbody>").appendTo(pv);
			if(arg ==0){		
				var pUrl = 'http://localhost:8080/shoppingmallrestful/rps?tid='+ $("#tenantId").val();
				 jQuery.ajax({
	                 type: "GET",
	          	   	  url: pUrl,
	                 dataType: 'jsonp',
	                 jsonpCallback: 'processData',
	                 success: function (data) {	
	                	 $("#objId").val(data.id);
	                     var appendStr = "";
	                     relAry = [];
				    		 	$.each(data.customFields, function(i, item){		
									if(item.indexType.value ==1){
								  	 	appendStr += "<tr><td><label>"+item.name+"</label></td>";
										appendStr += "<td><input type=\"text\" name=\""+ item.name+"\" value=\"\" class=\"text ui-widget-content ui-corner-all\" disabled /></td>";
										appendStr +="<input type='hidden' id='cfId' name='cfId' value='"+ item.id+"'></td></tr>";
									}else{
										 appendStr += doCustomField(item,i,arg);
									}
		     					});
				    		 pv.append($(appendStr));

			                 if(relAry.length > 0 ){
			                	 doCusRelSelect();
				             }

	                 },
	                 error:function (jqXHR, exception) {
	                     }
	             });
			}else{
				var pUrl = 'http://localhost:8080/shoppingmallrestful/rp?pid='+ arg +'&tid='+ $("#tenantId").val();
				 jQuery.ajax({
	                 type: "GET",
	          	   	  url: pUrl,
	                 dataType: 'jsonp',
	                 jsonpCallback: 'processData',
	                 success: function (data) {
	                	 $("#objId").val(data.objectId);
	                     	var appendStr = "";
	                        relAry = [];
				    		 $.each(data.customFields, function(i, item){	
				    			 if(item.name != 'ProductId' && item.name !='ProductName' && item.name !='UnitPrice'){
				    				 appendStr += doCustomField(item,i,arg);
				    				 
					    		  }else{
					    			  appendStr += "<tr><td><label>"+item.name+"</label></td>";
					    			  if(item.name =="ProductId"){
								    		appendStr += "<td><input type=\"text\" name=\"fieldValue\" value=\""+ item.value +"\" class=\"text ui-widget-content ui-corner-all\" disabled/>";
							    	 }else{
								    		appendStr += "<td><input type=\"text\" name=\"fieldValue\" value=\""+ item.value +"\" class=\"text ui-widget-content ui-corner-all\" />";
								   	}
					    			  appendStr +="<input type='hidden' id='cfId' name='cfId' value='"+ item.id+"'></td></tr>";
						    	}
		
		     				});
		     					
				    		 pv.append($(appendStr));
				    	     if(relAry.length > 0 ){
				               	doCusRelSelect();
					         }
	                
	                 },
	                 error:function (jqXHR, exception) {
	                     }
	             });
			}

			$( "#dialog-form" ).dialog( "open" );

		}

		function doCustomField(item, i,arg){
			var appendStr ="";
			var value="";
	    	if(item.value != null) value= item.value;
			if(item.type.value == 5){
				var ary = item.name.split("_");
				if(ary[0] != "Product"){
					relAry[relAry.length] = item.id;
					//console.log(ary[0])
	    	    	appendStr += "<tr><td><label name=\"fieldName\">"+ary[0]+"</label></td>";
	    	    	appendStr += "<td>&nbsp;<select name='selCr'></select>";
	    	    	appendStr += "<input type='hidden' id='cfType' name='cfType' value='"+ item.type.value +"'>";
	    	        if(arg != 0 ) appendStr += "<button name='btnViewRel' class='btn'>View</button>";
	    	        //console.log("Val" + value);
	    	        appendStr +="<input type='hidden' name='cfValue' value='"+ value +"'>";
	    	        appendStr +="<input type='hidden' name='relObjId' value=''>";
	    	        appendStr +="<input type='hidden' id='cfId' name='cfId' value='"+ item.id+"'></td></tr>";
	    		}else{
	    			if(arg !=0){
	        			appendStr += "<tr><td><label name=\"fieldName\">"+ary[1]+"</label></td>";
	        			appendStr += "<td>";
	        			appendStr += "<button name='btnViewRel' class='btn'>View</button>";
	        	        appendStr +="<input type='hidden' name='relObjId' value='"+  $("#objId").val() +"'>";
	        	        appendStr +="<input type='hidden' name='fieldValue' value='"+ value +"'>";
	        	        appendStr +="<input type='hidden' id='cfId' name='cfId' value='"+ item.id+"'></td></tr>";
	            	}		
		    	}
			}else{
		  	 	appendStr += "<tr><td><label>"+item.name+"</label></td>";
				if(item.type.value == 2){
					appendStr += "<td><input type=\"text\" name=\"fieldValue\" value=\""+ value+"\" class=\"date-input\" />";
				}else if(item.type.value == 6){
					appendStr += "<td><input type=\"hidden\" name=\"fieldValue\" value='"+ value+"'  /><button name='btnRtEdit' class='btn'>Edit</button>";
            	}else{
            		 appendStr += "<td><input type=\"text\" name=\"fieldValue\" value=\""+ value+"\" class=\"text ui-widget-content ui-corner-all\" />";
               	}
				appendStr += "<input type='hidden' id='cfType' name='cfType' value='"+ item.type.value +"'>";
			 	appendStr +="<input type='hidden' id='cfId' name='cfId' value='"+ item.id+"'></td></tr>";
		    }
		
		 	return appendStr;
		}

		function doCusRelSelect(){
		 	var rows = $("#productView tr:gt(0)"); // skip the header row
			var relUrl = 'http://localhost:8080/shoppingmallrestful/cr?tid='+ $("#tenantId").val()+"&cid="+$("#objId").val();
        	var crCo =null;
        	 $.when(jQuery.ajax({
	    	        type: "GET",
	    	 	   	  url:relUrl,
	    	     	  cache: false,
	    	        dataType: 'jsonp',
	    	        jsonpCallback: 'processData',
	    	        success: function (data) {
	    	        	crCo = data;
	    	        },
	    	        error:function (jqXHR, exception) {
	    	          }
	    	    })).done(function(a1){  
	    	    	doSelOption(0, rows , crCo);	
	    	    });

		}


		function doSelOption(i , rows , crCo){
			if(i >= relAry.length){
				return ;
			}
			
			var rowIndex = 0;
		        rows.each(function(index) {
		        	var cfId = $(this).find("input[name='cfId']").val();;
		        	if(cfId == relAry[i]) rowIndex = index;
	 	     });
			//console.log(rowIndex);
	   		 var scTr = $('#productView tr:eq(' +( rowIndex+1) + ')');
			var objName = (scTr).find("label[name='fieldName']").text(); 
			//console.log(objName);
			var oid = 0;
	   		$.each(crCo.customRelationships, function(i, item){	
	   			if(item.masterObjectName == objName){
	   				oid = item.masterObjectId;
	    	   	} 	
			});
			var crVal = $(scTr).find("input[name='cfValue']").val();
			$(scTr).find("input[name='relObjId']").val(oid);
			
		   //console.log(oid);
			 var sel =  $(scTr).find("select[name='selCr']");
			
			 var appendStr = "";
				var olistUrl = 'http://localhost:8080/shoppingmallrestful/colist?tid='+ $("#tenantId").val()+"&oid="+oid;
				//console.log(olistUrl);
		    	 $.when(jQuery.ajax({
		    	        type: "GET",
		    	 	   	  url:olistUrl,
		    	     	  cache: false,
		    	        dataType: 'jsonp',
		    	        jsonpCallback: 'processData',
		    	        success: function (data) {
		    	        	appendStr +="<option value='0'></option>";
			    	         $.each(data, function(i, item){
				    	         //console.log(item.name);
				    	         $.each(item.customFields, function(i, field){
					    	         if(field.fieldNum == 1){
						    	         //console.log(field.value);
					    	        	   if(item.pkCustomField.value == crVal){
							    	        	 appendStr +="<option value='"+ item.pkCustomField.value+"' selected>"+field.value+"</option>";
								    	     }else{
								    	    	 appendStr +="<option value='"+ item.pkCustomField.value+"'>"+field.value+"</option>";
									    	 }
							    	    }
				    	         	});
				    	    	});
			    	         
		    	        },
		    	        error:function (jqXHR, exception) {
		    	          }
		    	    })).done(function(a1){
		    	    	sel.append($(appendStr));
		    	    	doSelOption((i+1), rows , crCo);
		    	    });

		}

		
		//shcema
		function openSchmeaForm() {
			var pv = $("#productSchema");

			pv.empty();
			$("<thead><tr><th>Field Name</th><th>Field Type</th></tr></thead><tbody>").appendTo(pv);
			var pUrl = 'http://localhost:8080/shoppingmallrestful/rps?tid='+ $("#tenantId").val();
			 jQuery.ajax({
	              type: "GET",
	           	  url: pUrl,
	              dataType: 'jsonp',
	              jsonpCallback: 'processData',
	              success: function (data) {	
	                var appendStr = "";
			    	$("#objId").val(data.id);
				 	$.each(data.customFields, function(i, item){		

		    			if(item.name == 'ProductId' || item.name=='ProductName' || item.name=='UnitPrice'){

		    				appendStr += "<tr><td><input type='text' name='fieldName' value='"+item.name+"' disabled >";
			    			appendStr +="<input type='hidden' id='cfId' name='cfId' value='"+ item.id+"'></td>";
			    			appendStr += "<td>";

			    			if(item.type.value == 0){
			    				if(item.indexType.value ==1){
			    					appendStr += "<input type='text' name='fieldType' value='label' disabled >";
				    			}else{
				    				appendStr += "<input type='text' name='fieldType' value='textbox' disabled >";
					    		}	
			    			}else{
			    				appendStr += "<input type='text' name='fieldType' value='number textbox' disabled >";
					    	}

			    			console.log(item.type.value);
			    		
			    			appendStr += "<input type='hidden' id='fieldTypeVal' name='fieldTypeVal' value='"+ item.type.value+"'>";
			    			appendStr += "<input type='hidden' id='indexType' name='indexType' value='"+ item.indexType.value+"'></td></tr>";	
					 	}else{

					 		if(item.type.value != 5){
						 		appendStr += "<tr><td><input type='text' name='fieldName' value='"+item.name+"' />";
						 		//appendStr += "<Select  name='fieldName' style='display:none'></select>"
				    			appendStr +="<input type='hidden' id='cfId' name='cfId' value='"+ item.id+"'></td>";
				    			appendStr += "<td><Select id='fieldType' name='fieldType' >";

				    			//console.log(item.type.value);
				    			if(item.type.value == 0){
				    				appendStr += "<option value='0' selected>textbox</option>";
				    			}else{
				    				appendStr += "<option value='0'>textbox</option>";
				    			}
				    			if(item.type.value == 1){
				    				appendStr += "<option value='1' selected>number textbox</option>";
				    			}else{
				    				appendStr += "<option value='1'>number textbox</option>";
				    			}
				    			if(item.type.value == 2){
				    				appendStr += "<option value='2' selected>date</option>";
				    			}else{
				    				appendStr += "<option value='2'>date</option>";
				    			}
				    			
				    			if(item.type.value == 3){
				    				appendStr += "<option value='3' selected>image</option>";
				    			}else{
				    				appendStr += "<option value='3'>image</option>";
				    			}
	
				    			if(item.type.value == 4){
				    				appendStr += "<option value='4' selected>label</option>";
				    			}else{
				    				appendStr += "<option value='4'>label</option>";
				    			}

				    			if(item.type.value == 6){
				    				appendStr += "<option value='6' selected>rich text</option>";
				    			}else{
				    				appendStr += "<option value='6'>rich text</option>";
				    			}
	
				    			appendStr += "</Select>";
				    			appendStr += "<input type='hidden' id='indexType' name='indexType' value='"+ item.indexType.value+"'></td></tr>";	
					 		}
						}
		     			});
					 pv.append($(appendStr));
					
	         },
	          error:function (jqXHR, exception) {
                  }        
	             });
			 
			$( "#dialog-form2" ).dialog( "open" );

		}

		function addField() {
			var pv = $("#productSchema");	
			var appendStr = "<tr>";
			appendStr += "<td><input name='fieldName' type='text'></input></td>";
			appendStr += "<td><Select id='fieldType' name='fieldType' >";
			appendStr += "<option value='0'>textbox</option>";
	    	appendStr += "<option value='1'>number textbox</option>";
	    	appendStr += "<option value='2'>date</option>";
	    	appendStr += "<option value='3'>image</option>";
	    	appendStr += "<option value='4'>label</option>";
	    	appendStr += "<option value='6'>rich text</option>";
			appendStr += "</Select>";
			appendStr += "<input type='hidden' id='indexType' name='indexType' value='0'></td></tr>";	
			
			pv.append($(appendStr));

		}

		function saveProduct(){

	    	var json = '{"tid":'+ $("#tenantId").val() +',"customObjectId":'+$("#objId").val() +',"coPkVal":"'+$("#coPkVal").val()+'","customFields":[';
	    	$("#productView").find("tbody").find("tr").each(function() {
	    		var fieldId = $(this).find('input[name="cfId"]').val();
	    		var fieldValue = $(this).find('input[name="fieldValue"]').val();
	    		if($(this).find('input[name="cfType"]').val() == 6){
	    			fieldValue = JSON.stringify($('<div/>').text(fieldValue).html())
	    			fieldValue = fieldValue.substring(1,fieldValue.length-1);
		    	}
	    		
	
	    		if($(this).find('input[name="cfType"]').val() == 5){
	    			fieldValue = $(this).find('select[name="selCr"] option:selected').val();
	           	}
	    		
	    		json +='{"id":'+ fieldId +' ,"value":"'+fieldValue+'" },';
	    	});
	    	
	    	json = json.substring(0,json.length-1);
	    	json +="]}";
	
	    	//console.log(json);
	    	crossDomainPost("http://localhost:8080/shoppingmallrestful/scodata",json,true);

	    	
		}

		function saveSchema(){


	    	var json = '{"tid":'+ $("#tenantId").val() +',"customObjectId":'+$("#objId").val() +',"customFields":[';
			$("#productSchema").find("tbody").find("tr").each(function() {

				var fieldId = $(this).find('input[name="cfId"]').val();
	    		if(fieldId === undefined) fieldId=0;
	    		var fieldName = $(this).find('input[name="fieldName"]').val();
				
				var fieldType ;
				if(fieldName == 'ProductId' || fieldName =='ProductName' || fieldName =='UnitPrice'){
					fieldType = $(this).find('input[name="fieldTypeVal"]').val();
				}else{
					fieldType = $(this).find('select[name="fieldType"]').find(":selected").val();
				}
				var indexType = $(this).find('input[name="indexType"]').val();
				
				json +='{"id":'+ fieldId +' ,"name":"'+fieldName+'","type":"'+fieldType+'","indexType":"'+ indexType +'"},';
			});
			
			json = json.substring(0,json.length-1);
			json +="]}";

			//console.log(json);
			crossDomainPost("http://localhost:8080/shoppingmallrestful/scf",json,false);	

			
		}

	
		$("#btnM").click(function(){
			openSchmeaForm();
			$('#myModal').modal('show')
			
			});

		$("#btnRel").click(function(){
						
			$( "#relation" ).dialog("open");

			 var olistUrl = 'http://localhost:8080/shoppingmallrestful/comdlist?tid='+ $("#tenantId").val();
			 $("#relationlist").jqGrid("setGridParam", { datatype: "jsonp", url: olistUrl }).trigger("reloadGrid");
			});
	
	      jQuery("#relationlist").jqGrid({
	    	  url: 'local',
              datatype: "jsonp",
   			width:400,
   			height: 350,
   		   	colNames:['','Id','Name'],
   		   	colModel:[
 				{
                       name: 'selectedIso', formatter: "checkbox", formatoptions: { disabled: true }, editable: true,
                       edittype: "checkbox", width: 20
                   },
   		   		{name:'id',index:'id', width:60, sorttype:"int"},
   		   		{name:'name',index:'name', width:100},
   		   	],
   		   	caption: "Detail Objects",
   		   	rowNum:10,
   		   	rowList:[10,20,30],
   		 	onSelectRow: function (id, status) {
             //var rowData = jQuery(this).getRowData(id);
             var ch = jQuery(this).find('#' + id + ' input[type=checkbox]').prop('checked');
             if (ch) {
                 jQuery(this).find('#' + id + ' input[type=checkbox]').prop('checked', false);
             } else {
                 jQuery(this).find('#' + id + ' input[type=checkbox]').prop('checked', true);
             }
             rowChecked = 1;
             currentrow = id;
         },
         loadComplete: function () {
        	  var grid = $("#relationlist");
              var ids = grid.getDataIDs();
              var userUrl =  'http://localhost:8080/shoppingmallrestful/cr?tid='+ $("#tenantId").val()+'&cid='+$("#objId").val() ;  
              jQuery.ajax({
                  type: "GET",
           	   	  url:userUrl,
               	  cache: false,
                  dataType: 'jsonp',
                  jsonpCallback: 'processData',
                  success: function (data) {
                 	 $.each(data.customRelationships, function(i, item){	
         	        	 for (var j = 0; j < ids.length; j++) {
         	                 if (item.detailObjectId == ids[j] && item.detailObjectId != $("#objId").val()) {
         	                     grid.find('#' + ids[j] + ' input[type=checkbox]').prop('checked', true);
         	                     break;
         	                 }
         	             }
         				});
                  },
                  error:function (jqXHR, exception) {
                    }
              })

         }

   		});

	   	function saveRel(){

	   	 var grid = $("#relationlist"), ids = grid.getDataIDs(),val="";
         for (var j = 0; j < ids.length; j++) {
             var cellVal = grid.jqGrid('getCell', ids[j], 1);
             if (cellVal != 0) {
                 var ck = grid.find('#' + ids[j] + ' input[type=checkbox]').prop('checked');
                 if (ck) {
                     val += cellVal + ",";
                 }
             }
         }
         val = val.substring(0, val.length - 1);
       
         var json = '{"tid":'+ $("#tenantId").val() +',"cid":'+$("#objId").val() +',"coids":"'+ val +'"}';
     	 //console.log(json);
     	 crossDomainPost("http://localhost:8080/shoppingmallrestful/scr",json,false);

	}	


		 $('#productView').on('click','input' ,function() {

		        if($(this).attr("class") == "date-input"){
		        	$(this).datepicker('destroy').datepicker({showOn:'focus' ,dateFormat: 'yy/mm/dd'}).focus();
		          }
		        
		    });

	      var dialogRichtext = $( "#dialogRichtext" ).dialog({
	          autoOpen: false,
	          dialogClass: 'no-close',
	          modal: false,
	          width: 500,
	          buttons: {
	            OK: function() {
		            var index = parseInt($("#currentRowIndex").val())+1;
	            	  var scTr = $('#productView tr:eq(' + index + ')');
	                  var cfValue = $(scTr).find("input[name='fieldValue']");
	                  $(cfValue).val(tinyMCE.get("pRichText").getContent());
	                  $("#currentRowIndex").val("");
	                  tinyMCE.get("pRichText").setContent("");
	              $( this ).dialog( "close" );
	            
	            }
	          },
	          close: function() {
	          }
	        });
			
	      $("#productView").on("click" , "button",function(e){
	      	e.preventDefault();
	      	//console.log($(this).attr("name"));
				if($(this).attr("name") == "btnViewRel"){
					var scTr = $(this).parent().parent();
					var crVal = $(scTr).find("input[name='cfValue']").val();
					var oid = $(scTr).find("input[name='relObjId']").val();
					var cfId = $(scTr).find("input[name='cfId']").val();
					  var isEmpty = true;
					if(oid ==  $("#objId").val()){
						console.log("detail");
						var coDataUrl =  'http://localhost:8080/shoppingmallrestful/co?tid='+ $("#tenantId").val()+'&oid='+ $("#objId").val()+'&opk='+$("#coPkVal").val();
					            jQuery.ajax({
					                type: "GET",
					         	   	  url: coDataUrl,
					                dataType: 'jsonp',
					                jsonpCallback: 'processData',
					                success: function (data) {
						                
					                	  $.each(data.customFields, function(i, item){
						                	
											if(item.id == parseInt(cfId) && item.values !=null){
												
								    	    	if(typeof  item.values[0] != 'undefined'){
									    	    	    var title = item.values[0].name;
								    	    			 var cnStr = '[' , cmStr = '[';
								    	    			 $.each(item.values[0].customFields, function(i, item){
								    	    				 	if(item.type.value != 5){
								    	    				 		cnStr += '"'+ item.name +'",';
								    	    					 	cmStr +='{"index":"'+item.name +'","name":"'+item.name+'","search":false,"width":100},'
								    	    					}
								    	    		    });
								    	    			 isEmpty = false;
								    	    			 cnStr = cnStr.substring(0,cnStr.length -1) + ']';
										    			 cmStr = cmStr.substring(0,cmStr.length -1) + ']';
								    	    				jQuery('#rellist').jqGrid("GridUnload");
										    				jQuery('#rellist').jqGrid({
										    		    		jsonReader: { repeatitems: false, cell:"" },
										    		    		datatype: 'jsonstring',
										    		    		caption:title,
										    		    		colNames: jQuery.parseJSON(cnStr),
										    		    		colModel: jQuery.parseJSON(cmStr),
										    		    		viewrecords: true 
										    				});

										    				$.each(item.values, function(i, co){
											    				console.log(i);
								    	                		  var newRowData = '[{';
								    	                		  $.each(co.customFields, function(i, field){
								    	                    		  newRowData +='"'+ field.name+'":"'+ field.value+'",'
								    	                    		  });
								    	                		  newRowData = newRowData.substring(0,newRowData.length -1) + '}]';
								    	                		  console.log(newRowData);
								    	                		  $("#rellist").jqGrid('addRowData',1,jQuery.parseJSON(newRowData));
								    	    				});
										    				
													}

											}
										});				                	
						                },
						            error:function (jqXHR, exception) {
					                }
					            }); 
					}else{
						console.log("master");
	
						var coDataUrl =  'http://localhost:8080/shoppingmallrestful/co?tid='+ $("#tenantId").val()+'&oid='+ oid+'&opk='+crVal;
						  var handleCoData = function () {
					            jQuery.ajax({
					                type: "GET",
					         	   	  url: coDataUrl,
					                dataType: 'jsonp',
					                jsonpCallback: 'processData',
					                success: function (data) {
					                	var cnStr = '[' , cmStr = '[';
					                	var newRowData = '[{';
					                	  $.each(data.customFields, function(i, item){
					                		  if(item.type.value != 5){
						    				 		cnStr += '"'+ item.name +'",';
						    					 	cmStr +='{"index":"'+item.name +'","name":"'+item.name+'","search":false,"width":100},';
						    					    newRowData +='"'+ item.name+'":"'+ item.value+'",'
						    					 }
								            });
					                	  isEmpty = false;
					                		cnStr = cnStr.substring(0,cnStr.length -1) + ']';
						    			 	cmStr = cmStr.substring(0,cmStr.length -1) + ']';
						    			 	newRowData = newRowData.substring(0,newRowData.length -1) + '}]';

						    			 	jQuery('#rellist').jqGrid("GridUnload");
						    				jQuery('#rellist').jqGrid({
						    		    		jsonReader: { repeatitems: false, cell:"" },
						    		    		datatype: 'jsonstring',
						    		    		caption:data.name,
						    		    		colNames: jQuery.parseJSON(cnStr),
						    		    		colModel: jQuery.parseJSON(cmStr),
						    		    		viewrecords: true 
						    				});
						    				$("#rellist").jqGrid('addRowData',1,jQuery.parseJSON(newRowData));
						                },
						            error:function (jqXHR, exception) {
					                }
					            }) ;
					          };
								if(crVal != ""){
					       			handleCoData();
								}
					}

					if(isEmpty){
						jQuery('#rellist').jqGrid("GridUnload");
					}
					
		      		$( "#dialogRel" ).dialog("open");
				}else if($(this).attr("name") == "btnRtEdit"){
					dialogRichtext.dialog("open");
					var val = $(this).parent().parent().find("input[name='fieldValue']").val();
					tinyMCE.get("pRichText").setContent(val);
					$("#currentRowIndex").val( $(this).parent().parent().index());
				}
			
	      });

	  	function crossDomainPost(url, text,refersh) {
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

	    	if(refersh)setTimeout(function(){location.reload();},500);
		}
	      


	   
  });
})(jQuery);




</script>

<?php  $user =& JFactory::getUser();?>
<input type="hidden" id="userId" name="userId" value="<?php echo $user->id;?>" >
<input type="hidden" id="tenantId" name="tenantId" value="0" >
<input type="hidden" id="objId" name="objId" value="0" >
<input type="hidden" id="coPkVal" name="coPkVal" value="0" >
<input type="hidden" id="currentRowIndex" name="currentRowIndex" value="0" >
<p></p>
<h1 id="hTname"></h1>
<hr>
<span id="toolbar" >
	<button id="pAdd" class="btn btn-info">Add New Data</button>
	<button id="pEdit" class="btn btn-info" >Edit Product </button>	
	<button id="btnRel" class="btn btn-info" type="button">Edit Relationships</button>
</span>

<p></p>
<div>
<table id="list4"></table>
</div>



<div id="dialog-form" title="Product Data">
<div>
<table id="productView" class="ui-widget ui-widget-content">
</table>
  
<input type="hidden" id="hidProduct" name="hidProduct" value="" />
</div>
</div>

<div id="dialog-form2" title="Edit Product">
<button id="fAdd" class="btn">Add Field</button>
<p></p>
<p></p>
<table id="productSchema" class="ui-widget ui-widget-content">

  
</table>

</div>

<div id="dialogRel" title="Custom Relationship">
<table id="rellist" >
</table>
</div>





<div id="relation" title="Custom Relationships">

 <table id="relationlist"></table>
 
</div>


<div id="dialogRichtext" title="Rich Text Editor">

<textarea name="pRichText" id="pRichText" 
cols="0" rows="0" style="width: 100%; height: 250px;" 
class="mce_editable" aria-hidden="true"></textarea>
</div>



