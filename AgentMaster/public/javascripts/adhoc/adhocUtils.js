var adhocUtils = {
	generateChart : function(divId, queryId, aggId, queryParams,
			pointClickCallBack, pointExtraToolTip) {

		var scope = this;
		var url = '/adhoc/queryResultHisotry?queryId=' + queryId
				+ '&aggregationId=' + aggId;
		if (queryParams && queryParams.length > 0) {
			url += "&params=" + queryParams;
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
							flags[j].title = 'C';
							flags[j].text = scope.getCommentHtml(comment,
									commentBy, commentOn);
							j++;
						}
						if (pointClickCallBack) {
							points[i].events = {
								click : function(e) {
									pointClickCallBack(this);
								}
							}
						}

						prevId = curId;
						i++;
					} else if (prevId && prevId == curId) {
						if (comment && commentBy && commentOn) {
							flags[j - 1].text += '<br/>'
									+ scope.getCommentHtml(comment, commentBy,
											commentOn);
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
										var tip = Highcharts.dateFormat(
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
												+  Highcharts.numberFormat(
												this.y,0) + '</b>';
										if (pointExtraToolTip) {
											tip += '<br />' + pointExtraToolTip;
										}
										return tip;
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

	getCommentHtml : function(comment, commentBy, commentOn) {
		return '<strong>' + comment + '</strong><br/>' + '<samll>Posted by '
				+ commentBy + ' on ' + commentOn + '<small>';
	},

	getQueryURL : function(url, queryId, queryName,queryParams) {
		if ( url.indexOf("?")<0){
			url+='?';
		}
		if (url.indexOf("&")>0){
			url+="&";
		}
		if (queryId) {
			url += 'queryId=' + queryId;
		} else if (queryName) {
			url += 'queryName=' + queryName;
		}
		if (queryParams){
			url+='&params='+queryParams;
		}
		return url;
	}
}