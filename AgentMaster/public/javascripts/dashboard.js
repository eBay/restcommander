$(document).ready(function () {
			    	$(".filterbox").each(function(){
	    		$(this).click(populateTagOnClick);
	    	});
	    	
	    	$('input:checkbox[name=stratus]').each(function(){
	    		$(this).click(populateDataCenters);
	    	});
	    	$("#fromassetdate").monthpicker().bind("monthpicker-set-value", populateAssetAgeFilterTag);
	    	$("#toassetdate").monthpicker().bind("monthpicker-set-value", populateAssetAgeFilterTag);
	    	$("#skuFilter").multiselect().bind("multiselectclick multiselectcheckall multiselectuncheckall", populateAssetDataTagOnEvent);
	    	$("#dcFilter").multiselect().bind("multiselectclick multiselectcheckall multiselectuncheckall", populateAssetDataTagOnEvent);
	    	$("#orgFilter").multiselect().bind("multiselectclick multiselectcheckall multiselectuncheckall", populateAssetDataTagOnEvent);
	    	initTags();
	    });
			    
function populateDataCenters() {
	var stratus = [];
	$("#stratusCheckbox input:checked").each(function (){
		stratus.push(this.value);
	});
	
	//only stratus is selected
	var url;
	if(stratus.indexOf("Yes") >= 0 && stratus.indexOf("No")  == -1) {
		//get stratus data centers only.
		url = '/capacity/transparency/states/datacenters?showStratusDataCenters=true';
	}else {
		//get all data centers
		url = '/capacity/transparency/states/datacenters?showStratusDataCenters=false';
	}
	$.get(url).success(function(data) {
		var datacenters = data.datacenters;
		if(datacenters.length != document.getElementById("dcFilter").options.length) {
			//get a list of selected options
			var selectedOptions =  $.map($("#dcFilter").multiselect("getChecked"), function(input){
	             return input.value;
	         });
			
			//new options set
			var options;
			$(datacenters).each(function(index, value) {
				if(value != null) {
					options = options + '<option value="'+value+'"';
					
					//retaining the selections
					if(selectedOptions != null && selectedOptions.indexOf(value) >= 0) {
						options = options + " selected";
					}
					
					options = options + '>'+value+'</option>';
				}
			});						
			//remove all existing data centers.
			$("#dcFilter option").html("");
			$("#dcFilter").html(options);
			$("#dcFilter").multiselect("refresh");
		}
	});
}

function initTags() {
	$(".taggableFilter").each(function(index, element){
		populateTags(element);
	});
	populateAssetDataFilterTag($("#skuFilter"), $("#skuFilter").multiselect());
	populateAssetDataFilterTag($("#dcFilter"), $("#dcFilter").multiselect());
	populateAssetDataFilterTag($("#orgFilter"), $("#orgFilter").multiselect());
}

function populateTagOnClick(event) {
	if(!(event.target.id == "fromassetdate" || event.target.id == "toassetdate" ||
		 event.target.id == "skuFilter" || event.target.id == "dcFilter" || event.target.id == "orgFilter")) {			    		
		populateTags(event.target);
	}
}

function populateTags(element) {
	if(getElementId(element).indexOf("Stratus") >= 0) {
		populateStratusFilterTag(element)
	} else if($(element).hasClass("assetDateRange") || $(element).hasClass("assetYear")) {
		populateAssetAgeFilterTag(element);
	}
}

function populateAssetDataTagOnEvent(event, ui) {
	if(event != null && event.type == "multiselectuncheckall") {
		removeTag(event.target);
	}else {
		populateAssetDataFilterTag(event.target, $(this));
	}
}

function populateAssetDataFilterTag(element) {
 var val;
 var checkedValues = $.map($(element).multiselect("getChecked"), function(input){
     return input.value;
 });
 
 if(checkedValues.length == 0) {
	 return false;
 }
 
 if(checkedValues.length > 5) {
	 val = checkedValues.slice(0, 5).join(', ') + " and " + (checkedValues.length - 5) + " other options selected.";
 }else {
	 val = checkedValues.join(', ');
 }
 if(getElementId(element) == "skuFilter") {
	 val = "SKU: "+ val;
 }else if (getElementId(element) == "dcFilter") {
	 val = "Data Center: "+ val;
 }else if (getElementId(element) == "orgFilter") {
	 val = "Acct: "+ val;
 }
 if(findTag(element) != null) {
	 removeTag(element);
 }
 addTag(element, val);
}

function populateStratusFilterTag(element) {
	if($(element).is(':checked')) {
		
		if(findTag(element) == null) {
    		if(getElementId(element) == "Stratus") {
    			addTag(element, "Stratus");
    		} else if(getElementId(element) == "Non-Stratus") {
    			addTag(element, "Non-Stratus");
    		}
		}
		
	} else {
		removeTag(element);
	}
}

function populateAssetAgeFilterTag(element) {
	if($('input:radio[name=asset_filter]:checked').val() == 'assetDateRange') {
		if($('#fromassetdate').val() != null && $('#fromassetdate').val()!= '' &&
		   $('#toassetdate').val() != null && $('#toassetdate').val() != '') {			    			
			clearExistingAssetAgeTags();
			addTag($('#fromassetdate'), "Assets From " +$('#fromassetdate').val()+" - "+$('#toassetdate').val());
		}
	}else if($('input:radio[name=asset_filter]:checked').val() == 'assetNoOfYears') {
		clearExistingAssetAgeTags();
		addTag($('#Asset_Age_Year'), "Assets Less than " +$('#Asset_Age_Year').val()+" Years");
	}
}

function clearExistingAssetAgeTags() {
	if(findTag($('#fromassetdate')) != null) {
		removeTag($('#fromassetdate'));
	}
	
	if(findTag($('#Asset_Age_Year')) != null) {
		removeTag($('#Asset_Age_Year'));
	}
}

function findTag(element) {
	if($("#filterTags").find('#span_id_'+getElementId(element)).length == 0) {
		return null;
	}
	return $("#filterTags").find('#span_id_'+getElementId(element));
}

function getElementId(element) {
	var id = element.id;
	if(typeof id === 'undefined') {
		id = element.attr("id");
	}
	return id;
}

function addTag(element, val) {
	var value;
	var id = getElementId(element);
	if(val != null) {
		value = val;
	}else {
		value = element.value;
	}
	var html = '<span class="label label-info tag" id="span_id_'+id+'">'+ value + '</span>';
	$("#filterTags").append(html);
}

function removeTag(element) {
	$('#span_id_'+getElementId(element)).remove();
}
$(document).ready(function (){
	var list = $(".dataView").click(function(event){
		var id = event.delegateTarget.id;
		var level = id.split("_")[1];
		var assetState = id.split("_")[2];
		var assetStateToDisplay = id.split("_")[3];
		var domString = "";
		if(assetState.length > 10) {
			domString = "<'row-fluid'<'span12'<'assetTableHeader span4'><'export span1'><'span3'r> <'span4 assetTableLength'lf>>t<'row-fluid'<'span6'i><'span6'p>>";
		}else {
			domString = "<'row-fluid'<'span12'<'assetTableHeader span3'><'export span1'><'span4'r> <'span4 assetTableLength'lf>>t<'row-fluid'<'span6'i><'span6'p>>";
		}
		$('#orderedAssetData').hide();
		$('#assetData').show();
		$('#assetsTbl').dataTable({
			"sDom": domString,
			"bProcessing" : true,
			"bRetrieve" : false,
			"bPaginate" : true,
			"bServerSide" : true,
			"bSort" : false,
			"sPaginationType": "bootstrap",
			"bDestroy": true,
			"sAjaxSource" : "/capacity/transparency/states/assetdata?level="+level+"&state="+assetState+"&"+getFilterQueryString(),
			"sAjaxDataProp": "aaData",
			"iDisplayLength":50,
			"oLanguage": {
				"sSearch":"Search"
			},
			"aLengthMenu": [[5, 10, 25, 50, 100, -1], [5, 10, 25, 50, 100, "All"]],
			"fnDrawCallback": function(oSettings) {
				$(".asset_modal").each(function () {
					$(this).click(function (event) {
						event.preventDefault();
						var id = event.target.id;
						var assetId = id.substring(id.indexOf("modal_asset_") + "modal_asset_".length, id.length);
						var url = '/usage/asset_analysis_modal/' + assetId;
						$.get(url, function(data) {
								$(data).modal();
							});
					});
				});
			}
		});
		if(level == "L3") {
			if(assetStateToDisplay == 'Other') {
				$("div.assetTableHeader").html('<h3>Assets Reserved for Unknown Owners'+'</h3>');	
			}else {			
				$("div.assetTableHeader").html('<h3>Assets Reserved for ' +assetStateToDisplay +'</h3>');
			}
			
		}else {			
			if(assetState.indexOf('In') >= 0 || assetState.indexOf('in') >= 0) {
				$("div.assetTableHeader").html('<h3>Assets ' +assetStateToDisplay +'</h3>');
			}else {
				$("div.assetTableHeader").html('<h3>Assets In ' +assetStateToDisplay +'</h3>');
			}
		}
		$("div.export").html("<a href='#' id='exportToCSV' class='btn btn-link' title='Export to CSV'><img src='/public/images/ico/CSV.png'></a>");
		
		$("#exportToCSV").click(function() {
			window.open("/capacity/transparency/states/assetDataInCSV?level="+level+"&state="+assetState+"&"+getFilterQueryString(), "exportToCSV");
		});
	});
	
	var orderedList =  $(".orderedDataView").click(function(event){
		var id = event.delegateTarget.id;
		var level = id.split("_")[1];
		var assetState = id.split("_")[2];
		$('#assetData').hide();
		$('#orderedAssetData').show();
		$('#assetsOrderedTbl').dataTable({
			"sDom": "<'row-fluid'<'span12'<'assetTableHeader span3'><'exportOrdered span1'><'span4'r> <'span4 assetTableLength'l>>t<'row-fluid'<'span6'i><'span6'p>>",
			"bProcessing" : true,
			"bRetrieve" : false,
			"bPaginate" : false,
			"bServerSide" : true,
			"bSort" : false,
			"sPaginationType": "bootstrap",
			"bDestroy": true,
			"sAjaxSource" : "/capacity/transparency/states/ordered?level="+level+"&format=json&state="+assetState+"&"+getFilterQueryString(),
			"sAjaxDataProp": "aaData",
			"iDisplayLength":50,
			"aLengthMenu": [[5, 10, 25, 50, 100, -1], [5, 10, 25, 50, 100, "All"]]
		});
		$("div.assetTableHeader").html('<h3>Assets In ' +assetState +'</h3>');
		$("div.exportOrdered").html("<a href='#' id='exportToOrderedCSV' class='btn btn-link' title='Export to CSV'><img src='/public/images/ico/CSV.png'></a>");
		$("#exportToOrderedCSV").click(function() {
			window.open("/capacity/transparency/states/orderedDataInCSV?level="+level+"&state="+assetState+"&"+getFilterQueryString(), "exportToOrderedCSV");
		});
	});
});

$(document).ready(function(){
	   $("#orgFilter").multiselect({
		   hide:['',500],
		   show:['', 500],
		   height: 300,
		   selectedList: 3,
		   classes:"asset-filter"
	   }).multiselectfilter({width: 100});
});

$(document).ready(function(){
	   $("#skuFilter").multiselect({
		   hide:['',500],
		   show:['', 500],
		   height: 200,
		   selectedList: 4,
		   classes:"asset-filter"
	   }).multiselectfilter({width: 100});
});

$(document).ready(function(){
	   $("#dcFilter").multiselect({
		   hide:['',500],
		   show:['', 500],
		   height: 200,
		   selectedList: 4,
		   classes:"asset-filter"
	   }).multiselectfilter({width: 100});
});


function getFilterQueryString(){
	var dc=$("#dcFilter").val()==null?"":$("#dcFilter").val();
	var sku=$("#skuFilter").val()==null?"":$("#skuFilter").val();
	var org=$("#orgFilter").multiselect("getChecked").map(function() {
		return this.value;
	}).get();
	var stratus = [];
	$("#stratusCheckbox input:checked").each(function (){
		stratus.push(this.value);
	});
	if($('input:radio[name=asset_filter]:checked').val() == 'assetDateRange') {
		return "asset_filter=assetDateRange&fromassetdate="+$('#fromassetdate').val()+"&toassetdate="+$('#toassetdate').val()+"&stratus="+stratus+"&dc="+dc+"&model="+sku+"&org="+escape(org);
	}else if($('input:radio[name=asset_filter]:checked').val() == 'assetNoOfYears') {
		return "asset_filter=assetNoOfYears&asset_age="+$('#Asset_Age_Year').val()+"&stratus="+stratus+"&dc="+dc+"&model="+sku+"&org="+escape(org);
	}
	return "stratus="+stratus+"&dc="+dc+"&model="+sku+"&org="+escape(org);
}

$(function () {
	$("#stateTree").treeview({
		collapsed: true,
		animated: "medium",
		control:"#sidetreecontrol",
		persist: "location"
	});
	$("#filterAction").click(function() {
		 window.location="/capacity/transparency/states?"+getFilterQueryString();
	 })
	 
	$("#clearFilterAction").click(function(){
		window.location="/capacity/transparency/states?clearFilter=true"
	})
	 $("#L1States").click(function() {
		 $.get('/capacity/transparency/states/seriesdata/L1?'+getFilterQueryString()).success(redraw)
	 })
	 
	 $("#Documentation").click(function(){
		 popup = window.open('http://wiki2.arch.ebay.com/display/Ops/Asset+State+Definitions', 'Asset State Definitions', 'width=400,height=600,resizable=yes,scrollbars=yes');
		 popup.focus();
		 return false;
	 })
	 
	 $("a.assetState").popover(
			 {trigger:"hover"});
	 
	$("a.assetState").each(function() {
		$(this).click(function(event) {
			var id = event.delegateTarget.id;
			//parsing the id to determine the state level and value.
			var uncamelizedId = id.replace(/[A-Z]/g, '*$&');
			var stateLevel = uncamelizedId.split('*')[1];
			var stateValue = uncamelizedId.split('*').slice(2, uncamelizedId.split('*').length).join(' ');
			$.get('/capacity/transparency/states/seriesdata/'+stateLevel+'?state='+stateValue+"&"+getFilterQueryString() +"&groupby=dc").success(redraw)
		});
	});
	
	$("a.reservedCacheOwners").each(function () {
		$(this).click(function(event) {
			var id = event.delegateTarget.id;
			var stateLevel = id.substring(0, 2);
			var stateValue = id.substring(2, id.length);
			$.get('/capacity/transparency/states/seriesdata/'+stateLevel+'?state='+stateValue+"&"+getFilterQueryString() +"&groupby=dc").success(redraw)
		});
	});
})

function redraw(data){
		var seriesData = new Array();
		var sd = data.data;
		chart.setTitle({text: data.chartTitle})
		for(i = 0; i < sd.length; i++) {
			seriesData.push({name: sd[i].name, y: parseFloat(sd[i].value)})
		}
		chart.series[0].setData(seriesData);
		chart.redraw();
	}