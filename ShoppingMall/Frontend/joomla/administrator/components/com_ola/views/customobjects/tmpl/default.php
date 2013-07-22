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

	var tabTitle = $( "#tab_title" ),tabContent = $( "#tab_content" ),
    tabTemplate = "<li><a href='#{href}' id='#{id}'>#{label}</a></li>",
    tabCounter = 0;

	var initTab = function (title , isquery){
		/*
		colNames:['','objectId' ,'ProductId','ProductName', 'UnitPrice'],
		   	colModel:[
		{name:'delete',  formatter: function (cellvalue, options, rowObject) {
			 $("#objId").val(rowObject["objectId"]);
		    return '<a href="#" rel="'+rowObject["objectId"] +"/"+ rowObject["productId"] + '">delete</a>';
		},width:50},*/
		
		var hid = $("#divCus").find("input[name='"+ title +"']");
	 	var obj = jQuery.parseJSON(hid.val()); 
	 	var cnStr = '[';
	 	var cmStr = '[';
	 	$.each(obj.customFields, function(i, item){
	
			if(i == 0){
				cnStr += '"Del",';
				//cmStr +='{"name":"delete","formatter": "showlink", "width":50},';
				cmStr +='{"name": "selectedData", "formatter": "checkbox", "formatoptions": { "disabled": "false" }, "editable": "true","edittype": "checkbox", "width": 20},';
			}
		 	if(item.type.value != 5){
		 		cnStr += '"'+ item.name +'",';
			 	cmStr +='{"index":"'+item.name +'","name":"'+item.name+'","search":false,"width":100},'
			}
    	});
	 	cnStr = cnStr.substring(0,cnStr.length -1) + ']';
	 	cmStr = cmStr.substring(0,cmStr.length -1) + ']';

	 	jQuery('#list4').jqGrid("GridUnload");
		jQuery('#list4').jqGrid({
    		jsonReader: { repeatitems: false, cell:"" },
    		datatype: 'jsonstring',
    		height:'400',
    		caption:title,
    		colNames: jQuery.parseJSON(cnStr),
    		colModel: jQuery.parseJSON(cmStr),
    		viewrecords: true ,
    		ondblClickRow:function(id){
		    	 loadCustomObject(id);
		     }
		});

		$("#coName").val(obj.name);
		$("#coId").val(obj.id)
		if(!isquery){
			$("#keyword").val("");
			loadCustomObjects(obj.id);
		}else{
			loadByKw();
		}
	
	}
    


	$("#myTab").on("click", "a", function(e){
		  e.preventDefault();
		  $(this).tab('show');
		  initTab($(this).text(),false); 
	});
   

  	function loadCustomObject(id){
  		 var rowData = jQuery("#list4").jqGrid('getRowData', id );
  		 var colName = $("#coName").val()+"Id";
    	 openDataForm(rowData[colName]);
  	 }
    
    function loadCustomObjects(coId){
    	  var olistUrl = 'http://localhost:8080/shoppingmallrestful/colist?tid='+ $("#tenantId").val()+"&oid="+coId;
    	  var handleGrid = function () {
              jQuery.ajax({
                  type: "GET",
           	   	  url: olistUrl,
                  dataType: 'jsonp',
                  jsonpCallback: 'processData',
                  success: function (data) {
                      
                	  $.each(data, function(i, item){
                		  //var newRowData = '[{"delete":"delete",';
                		  var newRowData = '[{';
                		  $.each(item.customFields, function(i, field){
                    		  newRowData +='"'+ field.name+'":"'+ field.value+'",'
                    		  });
                		  newRowData = newRowData.substring(0,newRowData.length -1) + '}]';
                		  $("#list4").jqGrid('addRowData',1,jQuery.parseJSON(newRowData));
    					});
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
              });
              return false
          };
          handleGrid();

     }

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
  	  var olistUrl = 'http://localhost:8080/shoppingmallrestful/comdlist?tid='+ $("#tenantId").val();
  	  var handleCo = function () {
            jQuery.ajax({
                type: "GET",
         	   	  url: olistUrl,
                dataType: 'jsonp',
                jsonpCallback: 'processData',
                success: function (data) {
              	  $.each(data, function(i, item){
                  	  if(item.name != "Product"){
                      	  var hid = "<input type='hidden' id='"+ item.id+"' name='"+ item.name+"' value='"+ JSON.stringify(item) +"' >";
                      	  $("#divCus").append(hid);
      						addTab(item.name , item.id);
                      }
  					});
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
            });
            return false
        };
        handleCo();
	})
	
   var dialog = $( "#dialog" ).dialog({
      autoOpen: false,
      dialogClass: 'no-close',
      modal: true,
      buttons: {
        Add: function() {
            var text = '{\"name\":\"'+ $("#frmAddObject").find("input[name='object_name']").val() +'\",\"tid\":'+ $("#tenantId").val() +'}';
			crossDomainPost("http://localhost:8080/shoppingmallrestful/addco",text,true);
			//alert(text);
			//$("#hidText").val(text);
			//alert($("#hidText").val());
			//$("#frmAddO").submit();
			//window.location.reload();
          	$( this ).dialog( "close" );
          	
        },
        Cancel: function() {
          $( this ).dialog( "close" );
        }
      },
      close: function() {
        //form[ 0 ].reset();
      }
    });
    
    var dialogObjectEdit = $( "#dialogObjEdit" ).dialog({
        autoOpen: false,
        dialogClass: 'no-close',
        modal: true,
        width: 700,
        buttons: {
          OK: function() {
        	  saveSchema();
            $( this ).dialog( "close" );
            	
          },
          Cancel: function() {
            $( this ).dialog( "close" );
          }
        },
        close: function() {
          //form[ 0 ].reset();
        }
      });

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

    var dialogObjectData = $( "#dialogObjData" ).dialog({
        autoOpen: false,
        dialogClass: 'no-close',
        modal: true,
        width: 500,
        buttons: {
          OK: function() {
        	  saveData();
            $( this ).dialog( "close" );
            	
          },
          Cancel: function() {
            $( this ).dialog( "close" );
          }
        },
        close: function() {
          //form[ 0 ].reset();
        }
      });

    var dialogRelation = $( "#dialogRelation" ).dialog({
        autoOpen: false,
        dialogClass: 'no-close',
        modal: true,
        width: 500,
        buttons: {
          OK: function() {
        	  saveRelation();
            $( this ).dialog( "close" );
            	
          },
          Cancel: function() {
            $( this ).dialog( "close" );
          }
        },
        close: function() {
          //form[ 0 ].reset();
        }
      });

    

    // actual addTab function: adds new tab using the input from the form above
    function addTab(tabTitle , id) {
      var label = tabTitle,

        li = $( tabTemplate.replace( /#\{href\}/g, "#" ).replace( /#\{id\}/g, id ).replace( /#\{label\}/g, label ) );
        tabContentHtml = tabContent;
        $("#myTab").append(li);
      //tabs.find( ".ui-tabs-nav" ).append( li );
      //tabs.append( "<div id='" + id + "'><p>" + tabContentHtml + "</p></div>" );
      //tabs.tabs( "refresh" );
      if(tabCounter == 0) {
          $('#myTab a:first').tab('show');
          initTab(tabTitle);
      }
      tabCounter++;
    }
 
    // addTab button: just opens the dialog
    $( "#add_tab" ) .button() .click(function() {
        dialog.dialog( "open" );
      });
 
    $( "#editobject" ).button().click(function() {
    	openSchmeaForm();
    });

    $( "#addData" ).button().click(function() {
    	openDataForm(0);
    });

    
    $( "#fAdd" ).button().click(function(event) {
		event.preventDefault();
		addField();
	});

    function openSchmeaForm() {
        
    	var pv = $("#objSchema");
    	pv.empty();
    	$("<thead><tr><th>Field Name</th><th>Field Type</th><th>Index Type</th></tr></thead><tbody>").appendTo(pv);

    	var hid = $("#divCus").find("input[name='"+ $("#coName").val() +"']");
	 	var obj = jQuery.parseJSON(hid.val()); 

    		$.each(obj.customFields, function(i, item){			
        		if(item.type.value != 5){
	    			var appendStr = "<tr>";
	    			appendStr += "<td><input type='text' name='fieldName' value='"+item.name+"'></label>";
	    			appendStr +="<input type='hidden' id='cfId' name='cfId' value='"+ item.id+"'></td>";
	    			appendStr += "<td><Select name='fieldType'>";
	
	    			console.log(item.type.value);
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
	    			
	    			appendStr += "</Select></td>";
	    			appendStr += "<td><Select name='indexType'>";

	    			if(item.indexType.value == 1){
	    				appendStr += "<option value='1' selected>Primary Key</option>";
	    			}else{
	    				if(item.indexType.value == 0){
		    				appendStr += "<option value='0' selected>Not Indexed</option>";
		    			}else{
		    				appendStr += "<option value='0'>Not Indexed</option>";
		    			}

	    				if(item.indexType.value == 2){
		    				appendStr += "<option value='2' selected>Indexed</option>";
		    			}else{
		    				appendStr += "<option value='2'>Indexed</option>";
		    			}
	    			}
	    				    			
	    			appendStr += "</Select></td>";
	    			appendStr+="</tr>";
	    			pv.append($(appendStr));
        		}
    		});

    	
    	pv.append("</tbody>");
    	
    	dialogObjectEdit.dialog( "open" );
    }
    
    function addField() {
    	var pv = $("#objSchema");	
    	var appendStr = "<tr>";
    	appendStr += "<td><input name='fieldName' type='text'></input></td>";
    	appendStr += "<td><Select name='fieldType'>";
    	appendStr += "<option value='0'>textbox</option>";
    	appendStr += "<option value='1'>number textbox</option>";
    	appendStr += "<option value='2'>date</option>";
    	appendStr += "<option value='0'>image</option>";
    	appendStr += "<option value='0'>label</option>";
    	appendStr += "</td>";
    	appendStr += "<td><Select name='indexType'>";
    	appendStr += "<option value='0'>Not Indexed</option>";
    	appendStr += "<option value='2'>Indexed</option>";
    	appendStr += "</td></tr>";
    	pv.append($(appendStr));
    }

    function saveSchema(){
    	
    	var json = '{"tid":'+ $("#tenantId").val() +',"customObjectId":'+$("#coId").val() +',"customFields":[';
    	$("#objSchema").find("tbody").find("tr").each(function() {
    		var fieldId = $(this).find('input[name="cfId"]').val();
    		if(fieldId === undefined) fieldId=0;
    		var fieldName = $(this).find('input[name="fieldName"]').val();
    		var fieldType = $(this).find('select[name="fieldType"]').find(":selected").val();
    		var indexType = $(this).find('select[name="indexType"]').find(":selected").val();
    		
    		json +='{"id":'+ fieldId +' ,"name":"'+fieldName+'","type":"'+fieldType+'","indexType":"'+ indexType +'"},';
    	});
    	
    	json = json.substring(0,json.length-1);
    	json +="]}";

    	//console.log(json);
    	crossDomainPost("http://localhost:8080/shoppingmallrestful/scf",json,true);
		dialogObjectEdit.dialog( "close" );
		//todo reload problem
		
    }

	//data
	var relAry;
    function openDataForm(objPk) {
  
    	$("#coPkVal").val(objPk);
    	var pv = $("#objData");
    	pv.empty();
    	$("<thead><tr><th>Name</th><th>Value</th></tr></thead><tbody>").appendTo(pv);
		if(objPk == 0){
	    	var hid = $("#divCus").find("input[name='"+ $("#coName").val() +"']");
		 	var obj = jQuery.parseJSON(hid.val()); 
		 	var appendStr = "";
            relAry = [];
	    		$.each(obj.customFields, function(i, item){		
	    			appendStr += doCustomField(item,i,0);
	    		});
	    	pv.append($(appendStr));

            if(relAry.length > 0 ){
            	doCusRelSelect();
	        }	
	    	
		}else{					
			dialogObjectData.dialog('option', 'title', 'Edit Data');
			var coDataUrl =  'http://localhost:8080/shoppingmallrestful/co?tid='+ $("#tenantId").val()+'&oid='+ $("#coId").val()+'&opk='+objPk;
			  var handleCoData = function () {
		            jQuery.ajax({
		                type: "GET",
		         	   	  url: coDataUrl,
		                dataType: 'jsonp',
		                jsonpCallback: 'processData',
		                success: function (data) {
		                 var appendStr = "";
		                 relAry = [];
		              	  $.each(data.customFields, function(i, item){
		              		appendStr += doCustomField(item,i,objPk);
		  					});
		              		pv.append($(appendStr));
		              	  if(relAry.length > 0 ){
		                  	doCusRelSelect();
		      	          }	
		  					
		                },
		                error:function (jqXHR, exception) {
		                }
		            });
		            return false
		        };
		        handleCoData();
		}
		
		pv.append("</tbody>");
	    dialogObjectData.dialog( "open" );

    }

    function doCustomField(item, i,arg){
		var appendStr ="";
		var value="";
    	if(item.value != null) value= item.value;
		if(item.type.value == 5){
			var ary = item.name.split("_");
			if(ary[0] !=$("#coName").val()){
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
        	        appendStr +="<input type='hidden' name='relObjId' value='"+  $("#coId").val() +"'>";
        	        appendStr +="<input type='hidden' name='fieldValue' value='"+ value +"'>";
        	        appendStr +="<input type='hidden' id='cfId' name='cfId' value='"+ item.id+"'></td></tr>";
            	}		
	    	}
			
		}else{
	  	 	appendStr += "<tr><td><label>"+item.name+"</label></td>";
	  		if(item.indexType.value == 1){
	    		appendStr += "<td>&nbsp;<input type=\"text\" name=\"fieldValue\" value=\""+value +"\" class=\"text ui-widget-content ui-corner-all\" disabled />";
	  		}else if(item.type.value == 2){
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
	 	
		var relUrl = 'http://localhost:8080/shoppingmallrestful/cr?tid='+ $("#tenantId").val()+"&cid="+$("#coId").val();
		var rows = $("#objData tr:gt(0)"); // skip the header row
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
   		 var scTr = $('#objData tr:eq(' +( rowIndex+1) + ')');
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

    function saveData(){
    
    	var json = '{"tid":'+ $("#tenantId").val() +',"customObjectId":'+$("#coId").val() +',"coPkVal":"'+$("#coPkVal").val()+'","customFields":[';
    	$("#objData").find("tbody").find("tr").each(function() {
    		var fieldId = $(this).find('input[name="cfId"]').val();
    		var fieldValue = $(this).find('input[name="fieldValue"]').val();

    		if($(this).find('input[name="cfType"]').val() == 5){
    			fieldValue = $(this).find('select[name="selCr"] option:selected').val();
           	}
    		
    		json +='{"id":'+ fieldId +' ,"value":"'+fieldValue+'" },';
    	});
    	
    	json = json.substring(0,json.length-1);
    	json +="]}";

    	//console.log(json);
    	crossDomainPost("http://localhost:8080/shoppingmallrestful/scodata",json,true);
		dialogObjectEdit.dialog( "close" );

    }

    $("#editRelation").click(function(){

    	
    	$( "#dialogRelation" ).dialog("open");
		//$("#relationLabel").text("Master Object : " + $("#coName").val());
		 var olistUrl = 'http://localhost:8080/shoppingmallrestful/comdlist?tid='+ $("#tenantId").val();

		 $("#relationlist").jqGrid("setGridParam", { datatype: "jsonp", url: olistUrl }).trigger("reloadGrid");});

      jQuery("#relationlist").jqGrid({
    	  url: 'local',
          datatype: "jsonp",
			width:400,
			height: 350,
		   	colNames:['','Id','Name'],
		   	colModel:[
				{
                   name: 'selectedIso', formatter: "checkbox", formatoptions: { disabled: false }, editable: true,
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
         var userUrl =  'http://localhost:8080/shoppingmallrestful/cr?tid='+ $("#tenantId").val()+'&cid='+$("#coId").val() ;  
         jQuery.ajax({
             type: "GET",
      	   	  url:userUrl,
          	  cache: false,
             dataType: 'jsonp',
             jsonpCallback: 'processData',
             success: function (data) {
            	 $.each(data.customRelationships, function(i, item){		
    	        	 for (var j = 0; j < ids.length; j++) {
    	                 if (item.detailObjectId == ids[j] && item.detailObjectId != $("#coId").val()) {
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

   function saveRelation(){

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
       
         var json = '{"tid":'+ $("#tenantId").val() +',"cid":'+$("#coId").val() +',"coids":"'+ val +'"}';
     	 crossDomainPost("http://localhost:8080/shoppingmallrestful/scr",json,true);         

    };

    $("#delData").click(function(){
	
			 var grid = $("#list4");
	         var ids = grid.getDataIDs();
			var delIds = "";
	    	 for (var j = 0; j < ids.length; j++) {
	             if ( grid.find('#' + ids[j] + ' input[type=checkbox]').prop('checked')) {
	            	var id =  grid.jqGrid('getCell', ids[j], 1);
	            	delIds += id + ",";
	             }
	         }
	    	 delIds = delIds.substring(0, delIds.length-1);
	    	 var json = '{"tid":'+ $("#tenantId").val() +',"customObjectId":'+$("#coId").val() +',"coPkVals":"'+delIds+'"}';
	    	 //console.log(json);
	        crossDomainPost("http://localhost:8080/shoppingmallrestful/delcos",json,true);
     })

    $("#objData").on("click" , "button",function(e){
      	e.preventDefault();
      	//console.log($(this).attr("name"));
			if($(this).attr("name") == "btnViewRel"){
				
				var scTr = $(this).parent().parent();
				var crVal = $(scTr).find("input[name='cfValue']").val();
				var oid = $(scTr).find("input[name='relObjId']").val();
				var cfId = $(scTr).find("input[name='cfId']").val();
				  var isEmpty = true;
	
				if(oid ==  $("#coId").val()){
					//console.log(1);
					var coDataUrl =  'http://localhost:8080/shoppingmallrestful/co?tid='+ $("#tenantId").val()+'&oid='+ $("#coId").val()+'&opk='+$("#coPkVal").val();
				            jQuery.ajax({
				                type: "GET",
				         	   	  url: coDataUrl,
				                dataType: 'jsonp',
				                jsonpCallback: 'processData',
				                success: function (data) {
				                	  $.each(data.customFields, function(i, item){
										if(item.id == parseInt(cfId) && item.values != null ){
							    	    	if(typeof  item.values[0] != 'undefined'){
							    	    		   var title = item.values[0].name;
							    	    			 var cnStr = '[' , cmStr = '[';
							    	    			 $.each(item.values[0].customFields, function(i, item){
							    	    				 	if(item.type.value != 5){
							    	    				 		cnStr += '"'+ item.name +'",';
							    	    					 	cmStr +='{"index":"'+item.name +'","name":"'+item.name+'","search":false,"width":100},';
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
							    	                		  var newRowData = '[{';
							    	                		  $.each(co.customFields, function(i, field){
								    	                		  if(field.type.value == 6){
								    	                			  newRowData +='"'+ field.name+'":"",';
									    	                		}else{
									    	                			  newRowData +='"'+ field.name+'":"'+ field.value+'",';
										    	                	}
							    	                    		  });
							    	                		  newRowData = newRowData.substring(0,newRowData.length -1) + '}]';
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
					//console.log(2);
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
					    					 	if(item.type.value == 6){
					    					 		  newRowData +='"'+ item.name+'":"",';
						    					 }else{
						    						  newRowData +='"'+ item.name+'":"'+ item.value+'",';
							    				}
					    					  
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
      			
			}
		
      });
    
    $('#objData').on('click','input' ,function() {

        if($(this).attr("class") == "date-input"){
        	$(this).datepicker('destroy').datepicker({showOn:'focus' ,dateFormat: 'yy/mm/dd'}).focus();
          }
        
    });

    $("#query").click(function(){
    	initTab($("#coName").val(),true); 

    });

    function loadByKw(){

		 var olistUrl = 'http://localhost:8080/shoppingmallrestful/colistbk?tid='+ $("#tenantId").val()+'&oid='+$("#coId").val()+'&kw='+ $("#keyword").val();
		 //$("#list4").trigger("reloadGrid"); 
		 var handleGrid = function () {
             jQuery.ajax({
                 type: "GET",
          	   	  url: olistUrl,
                 dataType: 'jsonp',
                 jsonpCallback: 'processData',
                 success: function (data) {
                     
               	  $.each(data, function(i, item){
               		  var newRowData = '[{';
               		  $.each(item.customFields, function(i, field){
                   		  newRowData +='"'+ field.name+'":"'+ field.value+'",'
                   		  });
               		  newRowData = newRowData.substring(0,newRowData.length -1) + '}]';
               		  $("#list4").jqGrid('addRowData',1,jQuery.parseJSON(newRowData));
   					});
                 },
                 error:function (jqXHR, exception) {
                     }
             });
             return false
         };
         handleGrid();
    }

	function crossDomainPost(url, text , refersh) {
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
<input type="hidden" id="coName" name="coName" value="" >
<input type="hidden" id="coId" name="coId" value="" >
<input type="hidden" id="coPkVal" name="coPkVal" value="" >
<h1 id="hTname"></h1>
<ul id="myTab" class="nav nav-tabs">
    </ul>
<table>
<tr>
<td valign="top">

            <div class="btn-group btn-group-vertical">
      <button id="add_tab" class="btn btn-info">Add Object</button>
<button id="editobject" class="btn btn-info">Edit Object</button>
<button id="editRelation" class="btn btn-info">Edit Relationship</button>

            </div>
            <p></p>
                 <div class="btn-group btn-group-vertical">
                 <button  style="width: 128px" id="addData" class="btn btn-info">Add New Data</button>
<button id="delData" class="btn btn-info">Delete Data</button>
                 </div>
</td>
<td>
&nbsp;&nbsp;&nbsp;&nbsp;
</td>
<td>

    
    <div><input type="text" id="keyword" name="keyword"> <button id="query" class="btn">Query</button></div>
    <p></p>
    <p></p>
    <p></p>
       <p></p>
    <p></p>
       <p></p>
    <p></p>
  <table id="list4"></table>


</td>
</tr>
</table>
     <span>

 </span>
 <p></p>
 <p></p>


<div id="dialog" title="Object">
  <form id="frmAddObject"  >
    <fieldset class="ui-helper-reset">
      <label for="object_name">Name</label>
      <input type="text" name="object_name" id="object_name" value="" class="ui-widget-content ui-corner-all" />
    </fieldset>
  </form>
</div>

<div id="dialogObjData" title="Add Data">
  <form id="frmObjectData"  >
  
<table id="objData" class="ui-widget ui-widget-content">
</table>
  </form>
</div>

<div id="dialogObjEdit" title="Edit Object">
  <form id="frmEditObject"  >
  <button id="fAdd" style="font-size: 10px">Add Field</button>
<table id="objSchema" class="ui-widget ui-widget-content">
</table>
  </form>
</div>

<div id="dialogRel" title="Custom Relationship">
<table id="rellist" >
</table>
</div>
 
<div id="divCus">
</div>


<div id="dialogRelation" title="Custom Relationships">
  <table id="relationlist"></table>
  </div>
</div>


<iframe id="crossDomain" style="display: none">
<from id="form1" method="POST" action="" >
<input type="hidden" id="text" name="text" />
</from>
</iframe>

