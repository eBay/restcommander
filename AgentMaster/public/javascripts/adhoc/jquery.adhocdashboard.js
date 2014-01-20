(function($) {
	$.fn.adhocdashboard = function(options) {
		var errorCode = {
			too_Many_Columms : 1,
			undefined_component_type : 2,
			unsupported_component_type : 3,
			query_not_found : 4,
			aggregation_not_found : 5,
			query_not_run : 6
		};

		var componentsType = {
			sections : 'SECTIONS',
			section : 'SECTION',
			query : 'QUERY',
			aggregation : 'AGGREGATION',
			table : 'TABLE'
		};
		var eventType = {
			componentLoaded : "componentLoaded"
		};

		var data = {
			count : 0
		};
		var queryTypeDiv = $(".queryTypeDiv");
		var dashboardId = this[0].id;
		var queryTypeDiv = $(".queryTypeDiv");
		var modalPopupOK = $("#goal-details");
		var addWidget = $('#addDashboardWidget');
		var modalPopup = $('#modal-pkgalert');
		var closeModalPopup = $('#closeModalPopup');
		var columns = options.columns;
		var OriginalData = options;
		var span = 0;
		var methods = {
			trendingDivRunningCount : 0,
			loadingingDivRunningCount : 0,
			_getUniqueTrendingDivId : function() {
				// not collection on trending chart
				return dashboardId + 'Trending'
						+ (this.trendingDivRunningCount++);
			},
			_getUniqueloadingDiv : function() {
				// not collection on trending chart
				loadingDiv = $('<div class="componentLoading" id="'
						+ (this.loadingingDivRunningCount++) + '">');
				return loadingDiv;
			},
			_renderContainers : function(options) {
				if (!options.type) {
					return this
							._renderError(errorCode.undefined_component_type);
				} else if (options.type == componentsType.sections) {
					return this._renderSection(options);
				} else if (options.type == componentsType.section) {
					return this._renderSection(options);
				} else {
					return this
							._renderError(errorCode.unsupported_component_type);
				}

			},

			_renderAllComponents : function(options) {
				if (!options.type) {
					return this
							._renderError(errorCode.undefined_component_type);
				} else if (options.type == componentsType.query) {
					return this._renderQuery(options);
				} else if (options.type == componentsType.aggregation) {
					return this._renderAggregation(options);
				} else if (options.type == componentsType.table) {
					return this._renderTable(options);
				} else {
					return this
							._renderError(errorCode.unsupported_component_type);
				}
			},

			_renderError : function(code) {
				var html = '<div class="render-error">';
				if (code == errorCode.too_Many_Columms) {
					html += 'Too many columns!';
				} else if (code == errorCode.undefined_component_type) {
					html += 'Component type is not defined!';
				} else if (code == errorCode.unsupported_component_type) {
					html += 'Component type is not supported!';
				} else if (code == errorCode.query_not_found) {
					html += 'Query not found!';
				} else if (code == errorCode.aggregation_not_found) {
					html += 'Query aggregation is not found!';
				} else if (code == errorCode.query_not_run) {
					html += 'Error running this query!';
				}
				html += '</div>';
				return html;
			},

			_renderSections : function(options) {
				var settings = $.extend({
							columns : 4,
							title : '',
							containers : {}
						}, options);
				var maxColumns = 12, col = options.columns, content = $('<div class="sections dropme">'),numOfContainers = options.containers
						? options.containers.length
						: 0, curContainer;
				if (settings.title.length) {
					content.append('<div class="sectionsTitle">'
							+ settings.title + '</div>');
				}
				span = Math.floor((maxColumns / col));
				if (col && col > 0 && numOfContainers && numOfContainers > 0) {
					if (span <= 0) {
						content.append(this
								._renderError(errorCode.too_Many_Columms));
					} else {
						if (options.containers){
							options.containers.sort(function(a,b) { return parseFloat(a.position) - parseFloat(b.position) } );
						}
						content = this._renderSectionsContent(options, numOfContainers, col, span, false, content);
					}
				}
				return content;
			},
			
			_renderSectionsContent : function (options, numOfContainers, col, span, isAddWidget, content) {
				if(content == undefined) {
					content = $(".sections");
				}
				var canAddToExistingRow = false;
				var j=0;
				var i;
				var curContainer;
				
				while (j < numOfContainers) {
//					var row;
//					row = $('<div class="thumbnails dropme" style="margin-bottom:20px;">');
					for (i = 0; (i < col && j < numOfContainers); i++) {
						var divId = options.containers[j].title.split(" ").join("");
						curContainer = options.containers[j];
						content
								.append($('<div style="margin-bottom:10px;" id="sectionMainDiv-'+divId+'" class="marginClass sectionHover span' + span
										+ '" title="'+divId+'"><div class="hover-toggle pull-right deleteWidget handle" style="width:10px;height:10px;margin-right:10px;margin-top:15px;"><a href="#" title="Delete" ><i class="icon-trash"></i></a></div>')
										.append($('<div class="thumbnail">')
												.append(this
														._renderContainers(curContainer))));
						j++;
					}
					//content.append(row);
				}
				
				if(isAddWidget){
					$(".appendWidget").append(content);
				} else {
					return content;
				}
				
			},

			_renderSection : function(options) {
				var html = $('<div class="section">'), len, i,rowClass;
				var settings = $.extend({
							title : '',
							components : {}
						}, options);
				if (settings.title.length) {
					html.append('<div class="sectionTitle">' + settings.title + '</div>');
				}
				len = settings.components.length;
				for (i = 0; i < len; i++) {
					if ((i+1)%2==0){
						rowClass='even';
					}else{
						rowClass='odd';
					}
					html.append($('<div>').append(this
							._renderAllComponents(settings.components[i])));
				}

				return html;
			},

			_renderQuery : function(options) {
				var htmDiv = $('<div class="queryComponent">'), queryId = options.queryId, queryName = options.queryName, queryParams = options.queryParams, queryTitle, loadingDiv = this
						._getUniqueloadingDiv();

				urlQuery = '/adhoc/queryJson?';
				urlQuery = adhocUtils.getQueryURL(urlQuery, queryId, queryName,
						queryParams);
				htmDiv.append(loadingDiv);
				$.ajax({
							type : "get",
							url : urlQuery,
							dataType : "json",
							context : this,
							success : function(query, textStatus) {
								if (!query) {
									htmDiv
											.append(this
													._renderError(errorCode.query_not_found));
								} else {
									queryName = query.queryName;
									queryId = query.id;
									if (options.displayName
											&& options.displayName.length > 0) {
										queryTitle = options.displayName;
									} else {
										queryTitle = queryName;
									}
									var componentHtml = '<a target="_self" title="'
											+ query.description
											+ '" href="'
											+ adhocUtils.getQueryURL(
													'/adhoc/datatable',
													queryId, queryName,
													queryParams)
											+ '">'
											+ queryTitle + '</a>';
									htmDiv.append(componentHtml);
								}
							},
							complete : function() {
								$.event.trigger(eventType.componentLoaded,
										[loadingDiv]);
							}
						});
				return htmDiv;
			},

			_renderAggregation : function(options) {
				var settings = $.extend({
							aggregationMethod : 'COUNT',
							aggregationColumn : '*'
						}, options);
				var htmDiv = $('<div class="queryAggregation">'), queryId = settings.queryId, queryName = settings.queryName, queryParams = settings.queryParams, displayTitle, len, i, found = null, componentHtml, alertHtml, trendingDivId, loadingDiv = this
						._getUniqueloadingDiv();

				urlQuery = '/adhoc/queryAggregationJSON?';
				urlQuery = adhocUtils.getQueryURL(urlQuery, queryId, queryName,
						queryParams);
				htmDiv.append(loadingDiv);
				$.ajax({
					type : "get",
					url : urlQuery,
					dataType : "json",
					context : this,
					success : function(aggs, textStatus) {
						if (!aggs) {
							htmDiv
									.append(this
											._renderError(errorCode.aggregation_not_found));
						} else {
							len = aggs.length;
							for (i = 0; i < len; i++) {
								// find the aggregation
								if (settings.aggregationId
										&& settings.aggregationId > 0
										&& aggs[i].id == settings.aggregationId) {
									found = aggs[i];
									break;
								} else if (aggs[i].method == settings.aggregationMethod
										&& aggs[i].col == settings.aggregationColumn) {
									found = aggs[i];
									break;
								}
							}
							if (found) {
								if (settings.displayName
										&& settings.displayName.length > 0) {
									displayTitle = settings.displayName;
								} else {
									displayTitle = found.label;
								}
								
								
								
								// label
								componentHtml = '<span class="aggregationVal">';
								// alert if any
								if (found.alert) {
									alertHtml = '<img style="float:left;margin-right:3px;margin-top:15px;" src="/public/images/ico/alert_icon.png" title="'
											+ 'Assertion errors: '
											+ found.formatAlertMsg + '">';
									componentHtml += alertHtml;
								}
								componentHtml += '<h2>';
								componentHtml += Highcharts.numberFormat(found.value,0);
								componentHtml += '</h2></span>' + '</div>';
								
								
								
								componentHtml += '<div class="aggreation">'
									+ '<span class="aggregationLabel lead">'
									+ displayTitle + '</span>';
								// trending
								trendingDivId = this._getUniqueTrendingDivId();
								componentHtml += '<span class="aggregationTrendingSpan"><button class="btn btn-link btn-mini showTrending" name="'
										+ found.queryId
										+ '" value="'
										+ found.id
										+ '" params="'
										+ (queryParams ? queryParams : '')
										+ '" renderTo="'
										+ trendingDivId
										+ '">Show Trending</button>';
								componentHtml += '<button class="btn btn-link btn-mini hideTrending hide" name="'
										+ found.queryId
										+ '" value="'
										+ found.id
										+ '" renderTo="'
										+ trendingDivId
										+ '">Hide Trending</button></span>';
							
								
								
								componentHtml += '</div>';
								// trending div
								componentHtml += '<div id="' + trendingDivId
										+ '"></div>';
								
								
								htmDiv.append(componentHtml);
							} else {
								htmDiv
										.append(this
												._renderError(errorCode.aggregation_not_found));
							}
						}
					},
					error : function(textStatus) {
						htmDiv.append(this
								._renderError(errorCode.query_not_run));
					},
					complete : function() {
						$.event
								.trigger(eventType.componentLoaded,
										[loadingDiv]);
					}

				});
				return htmDiv;
			},

			_renderTable : function(options) {
				var settings = $.extend({
							columns : 4,
							rows : 100
						}, options);

				var htmDiv = $('<div class="tableComponent">'), queryId = settings.queryId, queryName = settings.queryName, queryParams = settings.queryParams, table, tableHtml, displayTitle, len, i, loadingDiv = this
						._getUniqueloadingDiv(),

				urlQuery = '/adhoc/queryColumnNamesJSON?';
				urlQuery = adhocUtils.getQueryURL(urlQuery, queryId, queryName,
						queryParams);
				if (settings.displayName && settings.displayName.length > 0) {
					htmDiv.append('<div class="tableTitle">'
							+ settings.displayName + '</div>');
				}
				htmDiv.append(loadingDiv);

				$.ajax({
					type : "get",
					url : urlQuery,
					dataType : "json",
					context : this,
					success : function(result, textStatus) {
						if (!result || !result.queryId) {
							htmDiv.append(this
									._renderError(errorCode.query_not_found));
						} else {
							queryId = result.queryId;
							if (result.columns) {
								table = $('<table class="table table-striped table-bordered table-condensed">');
								tableHtml = '<thead>' + '<tr>';
								len = result.columns.length;
								for (i = 0; (i < len && i < settings.columns); i++) {
									tableHtml += '<th>' + result.columns[i]
											+ '</th>';
								}
								tableHtml += '</tr>' + '</thead>' + '<tbody>'
										+ '</tbody>';
								htmDiv.append(table.append(tableHtml));
								var datatableAjaxSource = adhocUtils
										.getQueryURL(
												'/adhoc/datatableRecords?',
												queryId, queryName, queryParams)
										+ '&totalRecords='
										+ settings.rows
										+ '&numberOfColumns='
										+ settings.columns;
								table.dataTable({
											"sDom" : "t",
											"iDisplayLength" : settings.rows,
											"aaSorting" : [],
											"bServerSide" : true,
											"sAjaxSource" : datatableAjaxSource
										});
							}
						}
					},
					error : function(error) {
						htmDiv.append(this
								._renderError(errorCode.query_not_run));
					},
					complete : function() {
						$.event
								.trigger(eventType.componentLoaded,
										[loadingDiv]);
					}
				});
				return htmDiv;
			},
			
			_validateModalData : function() {
				methods._clearValidationErrors();
				var valid = true;
				var widgetTitle = $("#goalTitle");
				var widgetQueryRow = $( ".queryTypeRow" );
				
				if(!widgetTitle.val()) {
					widgetTitle.closest("div.control-group").addClass("error");
					widgetTitle.next("span.validationError").append("Title Cannot be empty");
				}
				
				
				widgetQueryRow.each(function( index ) {
					queryObject = new Object();
					var selectVal = $("select:first option:selected",this).val();
					if (selectVal == "query") {
						if($('span:first input', this).val() == "") {
							valid = false;
						}
					} else if(selectVal == "aggregation") {
						if ($('span:first input', this).val() == "") {
							valid = false;
						}
					} else {
						if ($('span:first input', this).val() == "") {
							valid = false;
						}
					}
					
					if(!valid) {
						$(this).closest("div.control-group").addClass("error");
						$("#aggValidationError-"+index).append("Query Cannot be empty");
					}
					
				});
				return valid;
			},
			
			_renderModalPopup : function (rowCount) {
				scope = this;
					var html  = '<div class="queryTypeRow control-group">';
					html += '<select class="input-small" name="inputQueryMethod" id="inputQueryMethod-'+rowCount+'">'
							+'<option value="query">Query</option>'
							+'<option value="aggregation">Aggregate</option>'
							+'<option value="table">Table</option>'
							+'</select>';
					html += '<span class="queryClass" id="queryName+'+rowCount+'"><input type="text" name="inputQueryTitle" id="inputQueryTitle-'+rowCount+'" class="span2" placeholder="Enter Query Name"></span>'
							+'<span class="help-inline validationError"></span>';
					html += '<span class="queryClass queryDisplayName" id="queryDisplayName+'+rowCount+'"><input type="text" name="inputQueryDisplayTitle" id="inputQueryDisplayTitle-'+rowCount+'" class="span2" placeholder="Display Name"></span>';
					html += '<span class="hide queryClass" id="inputAggMethod-'+rowCount+'"><select class="input-small" name="inputAggMethod">'
							+'<option value="COUNT">COUNT</option>'
				     		+'<option value="SUM">SUM</option>'
				     		+'<option value="AVG">AVG</option>'
				     		+'<option value="MIN">MIN</option>'
				     		+'<option value="MAX">MAX</option>'
							+'</select>';
					html += '<input class="input-small queryClass inputQueryAggCol" type="text" name="inputQueryAggCol" placeholder="column in query" value="*">';
					html += '<input class="input-small queryClass inputQueryAggDescription" type="text" name="inputQueryAggDescription" placeholder="Description">';
					html += '</span>';
					
					//html += '<input class="input-small queryClass" type="text" name="inputQueryParams" placeholder="params">'
					if(rowCount > 0){
						html += '<button class="removeAggBtn btn btn-link" title="Remove" id="removeQueryTypeBtn-'+rowCount+'"><i class="icon-minus"></i></button>';
					}
					//if(rowCount < 2){
					html += '<button id="addNewQueryTypeBtn-'+rowCount+'" class="btn btn-link" title="Add New Query Type"><i class="icon-plus"></i></button>';
					//}
					html += '<span class="help-inline validationError" id="aggValidationError-'+rowCount+'"></span>';
					html += '</div>';
					queryTypeDiv.append(html);
					$("#inputQueryMethod-"+rowCount).change(function(){
						var radioVal = $(this).val();
						if(radioVal == "aggregation"){
							$("#inputAggMethod-"+rowCount).show();
							$("#inputQueryDisplayTitle-"+rowCount).hide();
						} else if (radioVal == "query") {
							$("#inputAggMethod-"+rowCount).hide();
							$("#inputQueryDisplayTitle-"+rowCount).show();
						} else {
							$("#inputAggMethod-"+rowCount).hide();
							$("#inputQueryDisplayTitle-"+rowCount).hide();
						}
					});
					
					$("#addNewQueryTypeBtn-"+rowCount).click(function(){
						$(this).detach();
						data.count++;
						methods._renderModalPopup(data.count);
					});
					
					$("#removeQueryTypeBtn-"+rowCount).click(function(){
						$(this).parent().detach();
						if(data.count == rowCount) {
							data.count--;
							addBtn = '<button id="addNewQueryTypeBtn-'+(data.count)+'" class="addAggBtn btn btn-link" title="Add New Query Type"><i class="icon-plus"></i></button>';
							$("div.queryTypeRow:last").append(addBtn);
							$("#addNewQueryTypeBtn-"+(data.count)).click(function(){
								$(this).detach();
								data.count++;
								methods._renderModalPopup(data.count);
								
							});
						}
					})		
			},
			
			_bindEvents : function(){
				
				//event for invoking modal popup
				addWidget.click(function(){
					methods._clearModalPopupFields();
					methods._clearValidationErrors();
					modalPopup.modal('show');	
				});
				
				//add binding events to Modal Popup query type
				methods._renderModalPopup(data.count);
				modalPopupOK.click(function(){
					if(methods._validateModalData()) {
						modalPopup.modal('hide');
						methods.addWidgetToDashboard();
					}	
				});
				
				closeModalPopup.click(function() {
					modalPopup.modal('hide');
				});
				
				//drag and drop bind event
				$( ".dropme" ).sortable({
					//handle: '.handle',
					start: function(event, ui) {
			            var start_pos = ui.item.index();
			            ui.item.data('start_pos', start_pos);
			        },
					update: function(event, ui) { 
						var startPosition = ui.item.data('start_pos');
						var endPosition = ui.placeholder.index();
						var divOrder = $(this).sortable('toArray');
						methods.updateWidgetPosition(divOrder);
						methods._saveDashboard();
					}
				});	
			},
			
			_clearModalPopupFields : function () {
				$("#modal-pkgalert input[type=text]").val("");
				$(".queryTypeRow span.validationError").empty();
			},
			
			_clearValidationErrors : function() {
				$("div.control-group").removeClass("error");
				$("span.validationError").empty();
			},
			
			updateWidgetPosition: function(divOrder) {
				var dupData = OriginalData;
				//console.log(""+divOrder.len);
				for(var i=0;i<divOrder.length; i++){
					var divTitle = divOrder[i].split("sectionMainDiv-");
					for(var j=0; j< dupData.containers.length; j++) {
						var title = dupData.containers[j].title.split(" ").join("");
						if(divTitle[1] == title){
							OriginalData.containers[j].position = i;
						}	
					}
				}
			},
			
			_saveDashboard : function() {
				var postData = this._populateSaveJson();
					var url = "/adhoc/saveDashBoardQuery";
					$.ajax({
						type : "post",
						url : url,
						data : postData,
						dataType : "json",
						success : function(json, textStatus) {
						},
						error : function() {
							
						}
					});
			},
			
			_populateSaveJson : function() {
				var postData = {};
				postData.queryId = $(document).find('input[name="queryId"]').val();
				postData.dashboardDefinition = JSON.stringify(OriginalData);
				return postData;
			},
			
			addWidgetToDashboard : function() {
				this._constructWidgetJSONData();
				this._saveDashboard();
			},
			
			_constructWidgetJSONData : function() {	
				//var queryTypeLength = $(".queryTypeDiv > div").length;
				var title = $("#goalTitle").val();
				var dashboardSize =$(".sections > div").size();
				var componentObject = new Object();
				var queryObject;
				componentObject.title = title;
				componentObject.type = componentsType.section; 
				componentObject.position = dashboardSize+1;
				componentObject.components = [];
				$( ".queryTypeRow" ).each(function( index ) {
					queryObject = new Object();
					var selectVal = $("select:first option:selected",this).val();
					if (selectVal == "query") {
						queryObject.queryid = "0";
						queryObject.queryName = $('span:first input', this).val();
						var displayName = $('.queryDisplayName input',this).val();
						queryObject.displayName = displayName;
						queryObject.type = "QUERY";
					} else if(selectVal == "aggregation") {
						queryObject.queryid = "0";
						queryObject.queryName = $('span:first input', this).val();
						queryObject.type = "AGGREGATION";
						console.log($("select:last option:selected",this).val());
						queryObject.aggregationMethod = $("select:last option:selected",this).val();
						queryObject.aggregationColumn = $('span.inputQueryAggCol input', this).val() == "" || undefined ? "*" : $('span:last input', this).val();
						queryObject.displayName = $('span:last input:eq(1)', this).val();
					} else {
						queryObject.queryid = "0";
						queryObject.queryName = $('span:first input', this).val();
						queryObject.type = "TABLE";
						queryObject.columns = "3";
						queryObject.rows = "3";
					}
					componentObject.components.push(queryObject);
				});
				
				OriginalData.containers.push(componentObject);
				var pushedData = new Object();
				pushedData.containers = [componentObject]
				var containers = [componentObject];
				var myString = JSON.stringify(OriginalData);
				this._renderSectionsContent(pushedData, 1, columns, span, true);
			}
		};
		
		

		this.append($('<div class="adhocDashboard">').append(methods
				._renderSections(options)));
		this.append(methods._bindEvents());
		// bind events
		
		this.on("click.adhocdashboard","div.deleteWidget", null,function(e){
			if(confirm("Are you sure you want to delete this goal?")){
				 var deleteCount;
				 for(var i=0; i< OriginalData.containers.length; i++) {
					 var originalTitle = OriginalData.containers[i].title.split(" ").join("");
					 if($(this).parent().attr("title") === originalTitle){
						 deleteCount = i;
					 }
				 }
				 delete OriginalData.containers[deleteCount];
				 $(this).parent().remove();
				 OriginalData.containers.splice(deleteCount,"1");
				 myString = JSON.stringify(OriginalData);
				 methods._saveDashboard();
			 }	 
		});
		
		
		
		this.on("click.adhocdashboard", "button.showTrending", null,
				function(e) {
					e.preventDefault();
					// a href
					var btn = $(e.currentTarget);
					var queryId = btn.attr("name");
					var aggId = btn.attr("value");
					var params = btn.attr("params");
					var divId = btn.attr("renderTo");
					var div = $("div#" + divId);
					if (div.html().length <= 0) {
						adhocUtils.generateChart(divId, queryId, aggId, params);
						// div.append("queryid:=" + queryId);
						// aggregation
						// id:"+aggId);
					}
					div.show();
					btn.next("button.hideTrending").show()
					btn.hide();
				});

		// bind events
		this.on("click.adhocdashboard", "button.hideTrending", null,
				function(e) {
					e.preventDefault();
					// a href
					var btn = $(e.currentTarget);
					var queryId = btn.attr("name");
					var aggId = btn.attr("value");
					var divId = btn.attr("renderTo");
					var div = $("div#" + divId);
					div.hide();
					btn.hide();
					btn.prev("button.showTrending").show();
				});
		this.bind(eventType.componentLoaded, function(e, loadingDiv) {
					loadingDiv.remove();
				});

		return this;
	};

})(jQuery);
