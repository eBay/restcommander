var adhocDashboardResult = {
	queryId : null,
	dashboardDiv : null,
	commentsDiv : null,
	addWidget : null,
	dashboardDefinition : null,
	init : function() {
		this.commentsDiv = $('#commentsDiv');
		this.addWidget = $('#addDashboardWidget');
		this.modalPopup = $('#modal-pkgalert');
		this.dashboardDiv = $('#dashboardDiv');
		this.queryId = $('#queryId').val();
		this.dashboardDefinition = $.parseJSON($('#query').val());
	}
}

$(function() {
			if ($('#validJsonDef').val() == 'true') {
				adhocDashboardResult.init();
//				adhocDashboardResult.commentsDiv.usercomments({
//							bid : adhocDashboardResult.queryId,
//							btype : 'adhocQuery'
//						});

				adhocDashboardResult.dashboardDiv
						.adhocdashboard(adhocDashboardResult.dashboardDefinition);
			}
		});