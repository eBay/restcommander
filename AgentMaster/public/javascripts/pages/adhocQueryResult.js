var adhocQueryResult = {
	queryId : null,
	query : null,
	recordTotal : null,
	dataTable : null,
	columnTruncated : null,
	queryParams : null,
	sqlParamsStrInlineEdit : null,
	sqlParamsStrInlineEditDiv : null,
	sqlParamsStrInput : null,
	sqlParamsStrInputValidation : null,
	sqlParamsStrInputControlGroup : null,
	sqlParamsStrInputDiv : null,
	okChangeParameterBtn : null,
	cancelChangeParameterBtn : null,
	expectedSqlParamCount : 0,
	commentsDiv : null,

	/*getCommentHtml : function(comment, commentBy, commentOn) {
		return '<strong>' + comment + '</strong><br/>' + '<samll>Posted by '
				+ commentBy + ' on ' + commentOn + '<small>';
	},*/
	trendingCommentModal : {
		modal : null,
		title : null,
		pointId : null,
		pointX : null,
		pointY : null,
		chart : null,
		input : {
			comment : null
		},
		commentCtlGroup : null,
		commentValidationError : null,
		submitCommentBtn : null,
		show : function(point) {
			this.title.empty();
			// console.log(point);
			this.title.append('<h5>Data point [ '
					+ Highcharts.dateFormat('%A, %b %d %Y, %H:%M:%S', point.x)
					+ '    ' + point.series.name + ': ' + point.y + ' ]</h5>');

			this.pointId = point.id;
			this.pointX = point.x;
			this.pointY = point.y;
			this.chart = point.series.chart;
			this.input.comment.val('');
			this.commentCtlGroup.removeClass('error');
			// reset the form
			this.modal.modal('show');
		},
		hide : function() {
			this.modal.modal('hide');
		},
		init : function() {
			var scope = this;
			this.modal = $('#trendingCommentModal');
			this.title = $('#trendingCommentModalTitle');
			this.submitCommentBtn = $('#submitCommentBtn');
			this.input.comment = $('#trendingCommentForm_comment');
			this.commentCtlGroup = this.input.comment
					.closest('div.control-group');
			this.commentValidationError = this.input.comment
					.next("span.validationError");

			this.submitCommentBtn.click(function() {
				var comment = $.trim(scope.input.comment.val());
				if (!comment || comment.length == 0) {
					scope.commentCtlGroup.addClass('error');
					scope.commentValidationError.empty();
					scope.commentValidationError
							.append("Your comment is required.");
				} else {
					url = '/userComment/saveComment?btype=adhocQueryRunHistoryDetail'
							+ '&bid=' + scope.pointId + '&comment=' + comment;
					$.ajax({
						type : "get",
						url : url,
						dataType : "json",
						success : function(data) {
							if (data.error) {
								scope.commentValidationError.empty();
								scope.commentValidationError.append(data.error);
							} else {
								if (data.comment) {
									scope.hide();
									var flag = {};
									flag.x = scope.pointX;
									flag.y = scope.pointY;
									flag.title = 'C';
									flag.text = adhocUtils
											.getCommentHtml(
													data.comment.comment,
													data.comment.commentBy,
													data.comment.commentOn);
									scope.chart.series[1].addPoint(flag, true);
								}
							}
						},
						error : function() {
							scope.commentValidationError
									.append("Error submit comments.");
						}
					});

					// submit by ajax

				}

			});
		}
	},

	data : {
		historySqlParams : null
	},

	/*generateChart : function(divId, queryId, aggId) {

		var scope = this;
		var url = '/adhoc/queryResultHisotry?queryId=' + queryId
				+ '&aggregationId=' + aggId;
		if (this.queryParams && this.queryParams.length > 0) {
			url += "&params=" + this.queryParams;
		}
		$.ajax({
			type : "get",
			url : url,
			success : function(data) {
				// console.dir(data);
				var k, i = 0, j = 0, points = [], prevId, curId, flags = [], chartData = data.data, len = chartData.length, x, y, comment, commentBy, commentOn;
				for (k = 0; k < len; k++) {
					curId = chartData[k][0];
					x = chartData[k][1];
					y = chartData[k][2];
					comment = chartData[k][3];
					commentBy = chartData[k][4];
					commentOn = chartData[k][5];
					if (!prevId || prevId != curId) {

						points[i] = {};
						points[i].id = curId;
						points[i].x = x;
						points[i].y = y;

						if (comment && commentBy && commentOn) {
							flags[j] = {};
							flags[j].x = x;
							flags[j].y = y;
							flags[j].title = 'Comments';
							flags[j].text = adhocQueryResult.getCommentHtml(
									comment, commentBy, commentOn);
							j++;
						}
						points[i].events = {
							click : function(e) {
								if (!scope.trendingCommentModal.modal) {
									scope.trendingCommentModal.init();
								}
								scope.trendingCommentModal.show(this);
							}
						}
						prevId = curId;
						i++;
					} else if (prevId && prevId == curId) {
						if (comment && commentBy && commentOn) {
							flags[j - 1].text += '<br/>'
									+ adhocQueryResult.getCommentHtml(comment,
											commentBy, commentOn);
						}
					}
				}

				window.chart = new Highcharts.StockChart({
							chart : {
								renderTo : divId
							},

							rangeSelector : {
								selected : 1
							},

							title : {
								text : data.label
							},
							tooltip : {
								formatter : function() {
									// console.log(this);
									if (this.series
											&& this.series.name == 'Flag') {
										return this.point.text;
									} else {
										return Highcharts.dateFormat(
												'%A, %b %d %Y, %H:%M:%S',
												this.x)
												+ '<br />'
												+ '<span style="color:'
												+ this.points[0].series.color
												+ ';">'
												+ this.points[0].series.name
												+ '</span>'
												+ ': '
												+ '<b>'
												+ this.y
												+ '</b>'
												+ '<br />Click to add comments.';
									}
								}
							},
							series : [{
										name : data.label,
										data : points,
										id : 'dataseries'
									}, {
										type : 'flags',
										name : 'Flag',
										data : flags,
										// onSeries: 'dataseries',
										shape : 'squarepin'

									}

							]
						});
			}
		})
	},
*/
	getHistoryParams : function() {
		var scope = this;
		if (this.queryId && !this.data.historySqlParams) {
			this.data.historySqlParams = [];
			$.ajax({
						type : "get",
						url : "/adhoc/getHistorySqlParams?queryId="
								+ this.queryId,
						async : false,
						success : function(data) {
							if (data) {
								scope.data.historySqlParams = data;
							}
						},
						error : function() {
							scope.data.historySqlParams = [];
						}
					})
		}
		return this.data.historySqlParams;
	},

	getParameterCount : function(paramsStr) {
		if (paramsStr) {
			var paramsArr = paramsStr.split(";");
			return paramsArr.length;
		} else {
			return 0;
		}

	},

	onChangeParameters : function(newParams) {

		if (newParams && newParams == this.queryParams) {
			return this.resetParameters();
		}
		var newParamsCount = this.getParameterCount(newParams)
		if (newParamsCount == this.expectedSqlParamCount) {
			if (this.queryId) {
				url = "/adhoc/datatable?queryId=" + this.queryId + "&params="
						+ newParams;
			} else {
				url = "/adhoc/datatable?query=" + this.query + "&params="
						+ newParams;
			}
			window.location = url;
		} else {
			this.sqlParamsStrInputValidation
					.html("Wrong number of parameters.");
			this.sqlParamsStrInputControlGroup.addClass("error");
		}
	},

	resetParameters : function() {
		this.sqlParamsStrInlineEditDiv.show();
		this.sqlParamsStrInputDiv.hide();
	},

	init : function() {
		var scope = this;
		this.queryId = $('#queryId').val();
		this.query = $('#query').val();
		this.recordTotal = $('#recordTotal').val();
		this.columnTruncated = $('#columnTruncated').val();
		this.expectedSqlParamCount = $("#expectedSqlParamCount").val();
		this.queryParams = $('#queryParams').val();
		this.sqlParamsStrInlineEdit = $('#sqlParamsStrInlineEdit');
		this.sqlParamsStrInlineEditDiv = $('#sqlParamsStrInlineEditDiv');
		this.sqlParamsStrInput = $('#sqlParamsStrInput');
		this.sqlParamsStrInputValidation = $('#sqlParamsStrInputValidation');
		this.sqlParamsStrInputDiv = $('#sqlParamsStrInputDiv');
		this.okChangeParameterBtn = $('#okChangeParameterBtn');
		this.cancelChangeParameterBtn = $('#cancelChangeParameterBtn');
		this.sqlParamsStrInputControlGroup = $('#sqlParamsStrInputControlGroup');
		this.commentsDiv = $('#commentsDiv');

		var realParameterCount = this.getParameterCount(this.queryParams);
		if (this.sqlParamsStrInlineEditDiv) {
			if (this.expectedSqlParamCount != realParameterCount) {
				this.sqlParamsStrInlineEditDiv.hide();
			}
			this.sqlParamsStrInlineEdit.click(function() {
						scope.sqlParamsStrInlineEditDiv.hide();
						scope.sqlParamsStrInputDiv.show();
						scope.cancelChangeParameterBtn.show();
						scope.sqlParamsStrInput.val(scope.queryParams);
						scope.sqlParamsStrInput.focus();
					});
		}

		if (this.sqlParamsStrInputDiv) {
			if (this.expectedSqlParamCount == realParameterCount) {
				this.sqlParamsStrInputDiv.hide();
			}

			this.sqlParamsStrInput.autocomplete({
				source : function(request, response) {
					var source = scope.getHistoryParams();
					if (source) {
						// console.log(scope.getHistoryParams());
						response($.ui.autocomplete.filter(source, request.term));
					}
				}
			});

			this.sqlParamsStrInput.keydown(function(e) {
						// bind enter
						if (e.keyCode == 13) {
							e.preventDefault();
							scope.onChangeParameters($(this).val());
						};
					});
			this.okChangeParameterBtn.click(function(e) {
						e.preventDefault();
						scope.onChangeParameters(scope.sqlParamsStrInput.val());
					});

			this.cancelChangeParameterBtn.click(function(e) {
						e.preventDefault();
						scope.resetParameters();
					});
		}

		var datatableAjaxSource, exportURL;
		if (this.queryId) {
			datatableAjaxSource = '/adhoc/datatableRecords?queryId='
					+ this.queryId + '&totalRecords=' + this.recordTotal;
			exportURL = '/adhoc/exportCSV?queryId=' + this.queryId;
		} else if (this.query) {
			datatableAjaxSource = '/adhoc/datatableRecords?query=' + this.query
					+ '&totalRecords=' + this.recordTotal;
			exportURL = '/adhoc/exportCSV?query=' + this.query;
		}
		if (this.queryParams) {
			datatableAjaxSource += '&params=' + this.queryParams;
			exportURL += '&params=' + this.queryParams;
		}

		this.dataTable = $('#assetsTbl').dataTable({
			"sDom" : "<'row-fluid'<'span2'l><'span4'<'export'>>>r<'columnTruncateMsg'>t<'row-fluid'<'span6'i><'span6'p>>",
			"sPaginationType" : "bootstrap",
			"iDisplayLength" : 100,
			// "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100,
			// "All"]],
			// "bSort" : false,
			/* Disable initial sort */
			"aaSorting" : [],
			"oLanguage" : {
				"sLengthMenu" : "_MENU_ records"
			},
			"bProcessing" : true,
			"bServerSide" : true,
			// "sScrollX": "100%",
			// "bScrollCollapse": true,
			// "sScrollXInner": "110%",
			"sAjaxSource" : datatableAjaxSource
		});

		$("div.export")
				.html("<a href='#' id='exportToCSV' title='Export to CSV'><img style='vertical-align:top' src='/public/images/ico/CSV.png'></a>");
		$("#exportToCSV").click(function() {
					window.open(exportURL, "exportToCSV");
				});
		if (this.columnTruncated == 'true') {
			$("div.columnTruncateMsg")
					.html("<span class='text-warning'><small>Not all columns are shown, some columns are truncated due to page limitation. Please export to view all the columns.</small></span>");
		}

		$("button.showTrending").click(function() {
			var aggId = $(this).val();
			var divId = 'trending' + aggId;
			var div = $("div#" + divId);
			if (div.html().length <= 0) {
				adhocUtils.generateChart(divId, scope.queryId, aggId,
						scope.queryParams, function(e) {
							if (!scope.trendingCommentModal.modal) {
								scope.trendingCommentModal.init();
							}
							scope.trendingCommentModal.show(e);
						}, 'Click to add comments.');
				// div.append("queryid:="+scope.queryId).append("
				// aggregation
				// id:"+aggId);
			}
			div.show();
			$(this).hide();
			$(this).next("button.hideTrending").show();
		});

		$("button.hideTrending").click(function() {
					var aggId = $(this).val();
					var div = $("div#trending" + aggId);
					div.hide();
					$(this).hide();
					$(this).prev("button.showTrending").show();

				});

		$("button.showSQL").click(function() {
					var div = $("#SQLDiv");
					div.show();
					$(this).hide();
					$("button.hideSQL").show();
				});
		$("button.hideSQL").click(function() {
					var div = $("#SQLDiv");
					div.hide();
					$(this).hide();
					$("button.showSQL").show();
				});
		$("button.showHistorySqlParams").click(function() {
			var div = $("#historySqlParamsDiv");
			if (div.html().length <= 0) {
				// populate this div
				var hparams = scope.getHistoryParams();
				var html, i, url;
				html = '<pre><h4>All used parameters</h4>'

				if (!hparams || hparams.length <= 0) {
					html += "<span>No historical parameters found.</span>";
				} else {
					html += '<ul class=inline>';
					for (i = 0; i < hparams.length; i++) {
						url = "/adhoc/datatable?queryId=" + scope.queryId
								+ "&params=" + hparams[i];
						html += '<li><a target="_self" href="' + url + '">'
								+ hparams[i] + '</a></li>';
					}
					html += "</ul>";
				}
				html += '</pre>';
				div.html(html);
			}
			div.show();
			$(this).hide();
			$("button.hideHistorySqlParams").show();
		});
		$("button.hideHistorySqlParams").click(function() {
					var div = $("#historySqlParamsDiv");
					div.hide();
					$(this).hide();
					$("button.showHistorySqlParams").show();
				});
		this.commentsDiv.usercomments({
					bid : this.queryId,
					btype : 'adhocQuery'
				});

	}
};

$(function() {
			adhocQueryResult.init();
		});
