$(function() {
	var commentsDiv=$('#userCommentsDiv');
	var assetId=$('#assetId').val();
	commentsDiv.usercomments(
	{bid:assetId,
	 btype:'asset'
	}
);
});
