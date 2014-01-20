(function($) {
	$.fn.usercomments = function(options) {

		var commentInput, commentCtlGroup, commentValidationError, submitBtn, comment, commentListDiv, scope = this;

		// Create some defaults, extending them with any options that were
		// provided
		var settings = $.extend({
					bid : 'null',
					btype : 'null'
				}, options);

		var methods = {
			_validate : function() {
				comment = $.trim(commentInput.val());
				if (!comment || comment.length == 0) {
					commentCtlGroup.addClass('error');
					commentValidationError.empty();
					commentValidationError.append("Your comment is required.");
					return false;
				} else
					return true;
			},

			_getFormattedCommentList : function(commentList) {
				var list = '';
				for (var i = 0; i < commentList.length; i++) {
					list += this._getFormattedComment(commentList[i]);
					// console.log(list);
				}
				return list;
			},

			_getFormattedComment : function(comment) {
				return '<blockquote>' + '<p>' + comment.comment + '</p>'
						+ '<small>Posted by ' + comment.commentBy + ' on '
						+ comment.commentOn + '</small>' + '</blockquote>';
			},

			_submit : function() {
				url = '/userComment/saveComment?bid=' + settings.bid
						+ '&btype=' + settings.btype + '&comment=' + comment;
				$.ajax({
					type : "get",
					url : url,
					dataType : "json",
					success : function(data) {
						if (data.error) {
							commentValidationError.empty();
							commentValidationError.append(data.error);
						} else {
							if (data.comment) {
								commentListDiv
										.prepend(methods._getFormattedComment(data.comment));
								// clear it out
								commentInput.val('');
							}
						}
					},
					error : function() {
						scope.commentValidationError
								.append("Error submit comments.");
					}
				})
			},

			_loadComment : function() {
				url = '/userComment/listComments?bid=' + settings.bid
						+ '&btype=' + settings.btype;
				$.ajax({
							type : "get",
							url : url,
							dataType : "json",
							success : function(data) {
								if (data && data.length > 0) {
									commentListDiv
											.append(methods._getFormattedCommentList(data));
								}
							}
						})
			}
		};

		methods._loadComment();
		this
				.append("<h3>Comments</h3>")
				.append('<form>'
								+ '<fieldset>'
								+ '<div class="control-group">'
								+ '<div class="controls">'
								+ '<textarea class="usercomment" name="comment" size="255" rows="2" placeholder="Enter your comments..."></textarea>'
								+ '<span class="help-block validationError text-error"></span>'
								+ '</div>'
								+ '<div>'
								+ '<button class="btn btn-primary submitComment">Post</button>'
								+ '</div>' + '</div>' + '</fieldset>'
								+ '</form>')
				.append('<div class="commentList"></div>');

		commentInput = this.find('.usercomment');
		commentCtlGroup = this.find("div.control-group");
		commentValidationError = this.find('span.validationError');
		submitBtn = this.find('button.submitComment');
		commentListDiv = this.find('div.commentList');

		submitBtn.bind("click.usercomments", function(e) {
					e.preventDefault();
					if (methods._validate()) {
						methods._submit();
					}

				})

		return this;
	};

})(jQuery);
