var adhocAnalysis = {

	// constants
	ALLTAGID : -1,
	CERTIFIEDTAGID : -2,
	AUDITEDTAGID : -3,
	
	REPORTTYPE_Q: 'Query',
	REPORTTYPE_D : 'Dashboard',

	errorMsgs : {
		requiredMsg : 'Required field.'
	},

	// custom event
	events : {
		tagDataChange : "TAGDATACHANGE",
		tagCountChange : "TAGCOUNTCHANGE",
		filterDataChange : "FILTERDATACHANGE"
	},

	// cache the data
	data : {
		tags : null,
		filters : null,
		sortedTagArray : null,
		countChangeTagIds : null,
		filterCount : 0,
		certifiedCount : 0,
		auditedCount : 0,
		defaultRootTag : null,
		dbTables : null, // used for autocomplete on builing queries,
		// lazy load and load only once
		availQueryTbls : null, // used for autocomplete on builing queries,
		// lazy loading
		availQueryCols : {}
		// used for autocomplete on builing queries, lazy loading, it is a look
		// up table, key is the table name, values is the array of column names
	},

	alertDiv : null,
	alertContentDiv : null,

	// utility functions

	filterHasTag : function(aFilter, tagId) {
		if (aFilter && aFilter.tags) {
			var i, len = aFilter.tags.length;
			for (i = 0; i < len; i++) {
				if (aFilter.tags[i].map.tagId == tagId) {
					return true;
				}
			}
		}
		return false;
	},

	getTagTitleByName : function(name) {
		var i, title, tag;
		for (i in this.data.tags) {
			tag = this.data.tags[i];
			if (this.data.tags.hasOwnProperty(i) && tag.tag == name) {
				title = tag.description;
				if (!title) {
					title = tag.tag.substring(tag.tag.lastIndexOf(".") + 1);
				}
			}
		}
		return title;
	},

	getAvailQueryCols : function(tableName) {
		var result = null, i, len, found = false, urltable, value, type, description, table, tbls;
		if (this.data.availQueryCols[tableName]) {
			result = this.data.availQueryCols[tableName];
		} else {
			// found it the available tables
			tbls = this.getAvailQueryTbls();
			len = tbls.length;
			for (i = 0; i < len; i++) {
				if (tableName == tbls[i].value) {
					found = true;
					break;
				}
			}
			if (found) {
				// get from server
				// escape # sign it is not working on
				if (tableName.indexOf("#") >= 0) {
					urltable = tableName.replace(/#/g, '%23');
				} else {
					urltable = tableName;
				}
				$.ajax({
							type : "get",
							async : false,
							url : "/adhoc/getAvailableDBCols?tableName="
									+ urltable,
							dataType : "json",
							success : function(json) {
								if (json) {
									len = json.length;
									adhocAnalysis.data.availQueryCols[tableName] = [];
									for (i = 0; i < len; i++) {
										table = json[i][0];
										value = json[i][1];
										type = json[i][2];
										description = json[i][3];
										adhocAnalysis.data.availQueryCols[tableName]
												.push({
															value : value,
															type : type,
															description : description,
															table : table
														});
									}
								}
								result = adhocAnalysis.data.availQueryCols[tableName];
							},
							error : function() {
								// query has error, do not try to retrieve again
								adhocAnalysis.data.availQueryCols[tableName] = [];
								result = [];
							}
						});
			}

		}
		if (!result) {
			result = [];
			// return current avilable ones
			for (i in this.data.availQueryCols) {
				if (this.data.availQueryCols.hasOwnProperty(i)) {
					result = result.concat(this.data.availQueryCols[i]);
				}
			}
		}
		return result;

	},

	getAvailQueryTbls : function() {
		if (this.data.availQueryTbls) {
			return this.data.availQueryTbls;
		}
		this.data.availQueryTbls = [];
		var i, len, value, type, description;
		// get from cache first
		if (!this.data.dbTables) {
			$.ajax({
						type : "get",
						async : false,
						url : "/adhoc/getAvailableDBTables",
						dataType : "json",
						success : function(json) {
							if (json) {
								adhocAnalysis.data.dbTables = [];
								len = json.length;
								for (i = 0; i < len; i++) {
									value = json[i][0];
									type = json[i][1];
									description = json[i][2];
									adhocAnalysis.data.dbTables.push({
												value : value,
												type : type,
												description : description
											});
								}
							}
						}
					});

		}
		if (this.data.dbTables) {
			this.data.availQueryTbls = this.data.availQueryTbls
					.concat(this.data.dbTables);
		}

		if (this.data.filters) {
			len = this.data.filters.length;
			for (i = 0; i < len; i++) {
				value = '#' + this.data.filters[i].queryName + '#';
				type = 'Query';
				description = this.data.filters[i].description;
				this.data.availQueryTbls.push({
							value : value,
							type : type,
							description : description
						});
			}
		}
		return this.data.availQueryTbls;
	},

	addATagToTagData : function(tagJson) {
		var tagId = tagJson.id;
		if (tagId) {
			this.data.tags[tagId] = tagJson;
			if (tagJson.queries) {
				this.data.tags[tagId].count = tagJson.queries.length;
			} else {
				this.data.tags[tagId].count = 0;
			}
		}

	},

	init : function() {
		var scope = this;
		scope.alertDiv = $('div#filterActionAlert');
		scope.alertContentDiv = $('div#filterActionAlert div.content');
		scope.tagTree.init();
		scope.filterTab.init();
		scope.filterTable.init();
		scope.filterForm.init();
		scope.confirmDeleteModal.init();

		// load the data
		var url = "/adhoc/loadAllTagAndQueryList";
		$.ajax({
					type : "get",
					url : url,
					dataType : "json",
					context : this,
					success : function(json, textStatus) {
						this.data.tags = {};
						this.data.filters = {};
						// init the data
						var i, len = json.tagList.length, tagId, filterId;
						for (i = 0; i < len; i++) {
							this.addATagToTagData(json.tagList[i]);
							// default root
							if (json.tagList[i].defaultRoot) {
								this.data.defaultRootTag = json.tagList[i].tag;
							}
						}
						this.data.filters = json.filterList;
						len = json.filterList.length;
						for (i = 0; i < len; i++) {
							this.data.filterCount += 1;
							if (json.filterList[i].certified) {
								this.data.certifiedCount += 1;
							}
							if (json.filterList[i].audited) {
								this.data.auditedCount += 1;
							}
							/*
							 * if (json.filterList[i].tagId &&
							 * this.data.tags[json.filterList[i].tagId]) {
							 * this.data.tags[json.filterList[i].tagId].count +=
							 * 1; }
							 */
						}

						$.event.trigger(this.events.tagDataChange);
					}
				});

	},

	successAlert : function(msg) {
		var scope = this;
		scope.alertDiv.removeClass("alert-error").addClass("alert-success");
		scope.alertContentDiv.empty().html(msg);
		scope.alertDiv.show();
	},
	errorAlert : function(msg) {
		var scope = this;
		scope.alertDiv.removeClass("alert-success").addClass("alert-error")
		scope.alertContentDiv.empty().html(msg);
		scope.alertDiv.show();
	},

	deleteAFilter : function(filterId) {
		var i, len = this.data.filters.length, beforeFilter, found = false, tagId;
		this.data.countChangeTagIds = [];
		// find the old records
		for (i = 0; i < len; i++) {
			if (this.data.filters[i].id == filterId) {
				// delete it
				found = true;
				beforeFilter = this.data.filters[i];
				this.data.filters.splice(i, 1);
				break;
			}
		}
		// update count
		if (found) {
			this.data.filterCount -= 1;
			this.data.countChangeTagIds.push(this.ALLTAGID);
			len = beforeFilter.tags.length;
			for (i = 0; i < len; i++) {
				tagId = beforeFilter.tags[i].map.tagId;
				if (this.data.tags[tagId]) {
					this.data.tags[tagId].count -= 1;
					this.data.countChangeTagIds.push(tagId);
				}
			}
			$.event.trigger(this.events.tagCountChange);
			$.event.trigger(this.events.filterDataChange, [beforeFilter, null]);
		}
		adhocAnalysis.successAlert("Report is deleted successfully.");

	},
	addOrModifyAFilter : function(json) {
		if (!json) {
			return;
		}

		var filter = json.filter, tags = json.tags, i, len, beforeFilter, found = false, tag, oldTags, newTags, tagId;
		this.data.countChangeTagIds = [];
		// has new tag
		if (tags) {
			len = tags.length;
			for (i = 0; i < len; i++) {
				this.addATagToTagData(tags[i]);
			}
		}
		// find the old records
		len = this.data.filters.length;
		for (i = 0; i < len; i++) {
			if (this.data.filters[i].id == filter.id) {
				// swap them
				found = true;
				beforeFilter = this.data.filters[i];
				this.data.filters[i] = filter;
				break;
			}
		}
		if (!found) {
			// put it at the beginning
			this.data.filters.unshift(filter);
			this.data.filterCount += 1;
			this.data.countChangeTagIds.push(this.ALLTAGID);
			if (filter.audited) {
				this.data.auditedCount += 1;
				this.data.countChangeTagIds.push(this.AUDITEDTAGID);
			}
		}

		// update the count
		if ((found && !beforeFilter.certified && filter.certified)
				|| (!found && filter.certified)) {
			this.data.certifiedCount += 1;
			this.data.countChangeTagIds.push(this.CERTIFIEDTAGID);
		}

		if (found && filter.audited != beforeFilter.audited) {
			if (beforeFilter.audited) {
				this.data.auditedCount -= 1;
			} else {
				this.data.auditedCount += 1;
			}
			this.data.countChangeTagIds.push(this.AUDITEDTAGID);
		}

		if (found) {
			oldTags = beforeFilter.tags;
			len = oldTags.length;
			for (i = 0; i < len; i++) {
				tagId = oldTags[i].map.tagId;
				if (this.data.tags[tagId]) {
					this.data.tags[tagId].count -= 1;
					this.data.countChangeTagIds.push(tagId);
				}
			}
		}
		newTags = filter.tags;
		len = newTags.length;
		for (i = 0; i < len; i++) {
			tagId = newTags[i].map.tagId;
			if (this.data.tags[tagId]) {
				this.data.tags[tagId].count += 1;
				this.data.countChangeTagIds.push(tagId);
			}
		}

		if (tags) {
			$.event.trigger(adhocAnalysis.events.tagDataChange);
		} else if (this.data.countChangeTagIds.length > 0) {
			$.event.trigger(adhocAnalysis.events.tagCountChange);
		}

		$.event.trigger(adhocAnalysis.events.filterDataChange, [beforeFilter,
						filter]);

		if (found) {
			adhocAnalysis.successAlert("Report is modified successfully.");
		} else {
			adhocAnalysis.successAlert("Report is added successfully.");
		}
		adhocAnalysis.filterTab.hideEditFilterTab();
	},

	deleteATag : function(json) {
		var i, len;
		if (json.delTag) {
			delete this.data.tags[json.delTag.id];
		}
		if (json.modTags) {
			len = json.modTags.length;
			for (i = 0; i < len; i++) {
				this.addATagToTagData(json.modTags[i]);
			}
		}
		$.event.trigger(this.events.tagDataChange);
	},

	tagTree : {
		currentTagLi : null,
		currentTagId : null,
		deleteTagId : null,
		tree : null,
		reportNav : null,

		confirmDeleteTagModal : {
			modal : null,
			confirmDeleteTagBtn : null,
			init : function() {
				var scope = this;
				scope.modal = $("#confirmDeleteTagModal");
				scope.confirmDeleteBtn = $("#confirmDeleteTagBtn");
				scope.confirmDeleteBtn.click(function() {
							scope.modal.modal('hide');
							adhocAnalysis.tagTree.deleteATag();
						});
			},
			show : function() {
				this.modal.modal('show');
			}
		},

		editTagModal : {
			modal : null,
			saveTagBtn : null,
			tagData : null,
			inputs : {
				tag : null,
				description : null,
				email : null
			},

			validationErrors : {
				tag : null
			},

			controlGroups : {
				tag : null
			},
			init : function() {
				var scope = this;
				scope.modal = $("#editTagModal");
				scope.inputs.tag = $('#editReportTagForm_tagName');
				scope.inputs.description = $('#editReportTagForm_description');
				scope.inputs.email = $('#editReportTagForm_email');

				scope.validationErrors.tag = scope.inputs.tag
						.next("span.validationError");

				scope.controlGroups.tag = scope.inputs.tag
						.closest("div.control-group");
				scope.saveTagBtn = $("#saveTagBtn");
				scope.saveTagBtn.click(function() {
							adhocAnalysis.tagTree.saveATag();
						});
			},
			show : function() {
				this.modal.modal('show');
			},
			hide : function() {
				this.modal.modal('hide');
			}
		},

		saveATag : function() {
			// validate first
			var scope = this;
			var duplicateTagNameMessage = "Tag name is used already. Tag name must be unique.";
			var invalidParentTagMessage = "User defined tag can not be a root tag and must have a parent tag.";
			var tagData = this.editTagModal.tagData;
			tagData.tag = this.editTagModal.inputs.tag.val();
			tagData.description = this.editTagModal.inputs.description.val();
			tagData.mailingList = this.editTagModal.inputs.email.val();

			var valid = true;
			if (!tagData.tag) {
				this.editTagModal.validationErrors.tag
						.html(adhocAnalysis.errorMsgs.requiredMsg);
				valid = false;
			} else if (!tagData.system) {
				tagData.tag = tagData.tag.toUpperCase();
				$.ajax({
							type : "post",
							data : tagData,
							async : false,
							url : "/adhoc/validateTagName",
							dataType : "text",
							success : function(text, textStatus) {
								if ("duplicate" == text) {
									scope.editTagModal.validationErrors.tag
											.append(duplicateTagNameMessage);
									valid = false;
								} else if ("invalidParent" == text) {
									scope.editTagModal.validationErrors.tag
											.append(invalidParentTagMessage);
									valid = false;
								}
							}
						});
			}
			if (!valid) {
				this.editTagModal.controlGroups.tag.addClass("error");
			}

			else {
				$.ajax({
					type : "post",
					data : tagData,
					url : "/adhoc/saveTag",
					dataType : "json",
					success : function(tags) {
						if (tags != null && tags.length > 0) {
							for (var i = 0; i < tags.length; i++) {
								adhocAnalysis.addATagToTagData(tags[i]);
							}
							$.event.trigger(adhocAnalysis.events.tagDataChange);
						}
						scope.editTagModal.hide();
					}
				});

			}
		},

		init : function() {
			var scope = this;
			scope.tree = $("#tagTree");
			scope.reportNav = $("#reportNav");
			if ($.isNumeric($.trim($("#initTagId").val()))) {
				scope.currentTagId = $("#initTagId").val(); // init only once
			}

			// bind the event when tag click
			scope.reportNav.on("click", "li a.tag", null, function(e) {
						e.preventDefault();
						// a href
						var tagId = e.currentTarget.name;
						scope.onSelectATreeNode(tagId);
					});
			// bind the event when tag hover
			scope.tree.on("mouseenter", "li a.tag", null, function(e) {
						var tagRef = $(this);
						var removeTag = tagRef.find("span.deleteTag");
						if (removeTag) {
							removeTag.show();
						}
						var editTag = tagRef.find("span.editTag");
						if (editTag) {
							editTag.show();
						}
					});
			scope.tree.on("mouseleave", "li a.tag", null, function(e) {
						var tagRef = $(this);
						var removeTag = tagRef.find("span.deleteTag");
						if (removeTag) {
							removeTag.hide();
						}
						var editTag = tagRef.find("span.editTag");
						if (editTag) {
							editTag.hide();
						}
					});
			scope.tree.on("click", "li a.tag > span.deleteTag", null, function(
							e) {
						var tagId = $(e.currentTarget).closest("a.tag")
								.attr("name");
						scope.beforeDeleteATag(tagId);

					});
			scope.tree.on("click", "li a.tag > span.editTag", null,
					function(e) {
						var tagId = $(e.currentTarget).closest("a.tag")
								.attr("name");
						scope.onEditATag(tagId);

					});

			scope.tree.bind(adhocAnalysis.events.tagDataChange, function(e) {
				// rebuild sorted tag array
				adhocAnalysis.data.sortedTagArray = [];
				for (i in adhocAnalysis.data.tags) {
					if (adhocAnalysis.data.tags.hasOwnProperty(i)) {
						adhocAnalysis.data.sortedTagArray
								.push(adhocAnalysis.data.tags[i].tag);
					}
				}
				adhocAnalysis.data.sortedTagArray.sort();
				scope.redrawTree();
				var tagId = scope.currentTagId;
				if (!tagId) {
					tagId = adhocAnalysis.CERTIFIEDTAGID;
				}
				// pick the selected tag
				scope.reportNav.find('a.tag[name=' + tagId + ']')
						.trigger("click");
					// adhocAnalysis.filterForm.populateTagSelect();
				});
			scope.tree.bind(adhocAnalysis.events.tagCountChange, function(e) {
						scope
								.redrawCountOnly(adhocAnalysis.data.countChangeTagIds);

					});
			scope.tree.bind(adhocAnalysis.events.filterDataChange, function(e,
					beforeFilter, afterFilter) {
				adhocAnalysis.filterTable.redraw(scope.currentTagId);

				if (!beforeFilter
						|| !afterFilter
						|| (beforeFilter.queryName != afterFilter.queryName)
						|| (beforeFilter.description != afterFilter.description)) {
					// clear out the avilable db table names for query
					// autocomplete
					// since the template query names changed
					adhocAnalysis.data.availQueryTbls = null;
				}

				// remove the cached column name
				if (!afterFilter
						|| (beforeFilter && ((beforeFilter.queryName != afterFilter.queryName) || (beforeFilter.query != afterFilter.query)))) {
					if (beforeFilter
							&& adhocAnalysis.data.availQueryCols
									.hasOwnProperty("#"
											+ beforeFilter.queryName + "#")) {
						delete adhocAnalysis.data.availQueryCols["#"
								+ beforeFilter.queryName + "#"];
					}
				}

			});
			scope.confirmDeleteTagModal.init();
			scope.editTagModal.init();
		},

		beforeDeleteATag : function(tagId) {
			this.deleteTagId = tagId;
			this.confirmDeleteTagModal.show();
		},

		onEditATag : function(tagId) {
			var scope = this;
			// load a json
			$.get('/adhoc/loadTag?tagId=' + tagId, function(tagData) {
				// load this tag for edit
				if (tagData) {
					scope.editTagModal.tagData = tagData;
					scope.editTagModal.inputs.tag.val(tagData.tag);
					scope.editTagModal.inputs.description
							.val(tagData.description);
					scope.editTagModal.inputs.email.val(tagData.mailingList);
					if (tagData.system) {
						scope.editTagModal.inputs.tag.attr("disabled", "true");
						scope.editTagModal.inputs.description.attr("disabled",
								"true");
					} else {
						scope.editTagModal.inputs.tag.removeAttr("disabled");
						scope.editTagModal.inputs.description
								.removeAttr("disabled");
					}

					scope.editTagModal.validationErrors.tag.empty();
					scope.editTagModal.controlGroups.tag.removeClass("error");
					scope.editTagModal.show();
				} else {
					scope.editTagModal.tagData = tagData;
					adhocAnalysis
							.errorAlert("This tag does not exist anymore, please refresh the page to get the lastest tag view.");
				}
			});

		},

		deleteATag : function() {
			var url = "/adhoc/deleteTag?tagId=" + this.deleteTagId;
			$.ajax({
						type : "get",
						url : url,
						dataType : "json",
						success : function(json, textStatus) {
							adhocAnalysis.deleteATag(json);
							adhocAnalysis
									.successAlert("Tag is deleted successfully.");
						},
						error : function() {
							adhocAnalysis.errorAlert("Error delete a tag.");
						}
					});
		},

		onSelectATreeNode : function(tagId) {
			var scope = this;
			if (!tagId) {
				tagId = scope.currentTagId;
			}

			if (scope.currentTagLi) {
				scope.currentTagLi.removeClass("active");
			}
			nodeLi = $("a.tag[name='" + tagId + "']").closest("li");

			scope.currentTagLi = nodeLi;
			scope.currentTagLi.addClass("active");
			if (tagId != scope.currentTagId) {
				scope.currentTagId = tagId;
			}
			// re-load filter list
			adhocAnalysis.filterTable.redraw(tagId);

			adhocAnalysis.filterTab.showFilterListTab();
		},

		findDataByTagId : function(tagId) {
			var i = 0;
			for (i = 0; i < this.numOfNodes; i++) {
				if (this.data[i].id == tagId) {
					return this.data[i];
				}
			}
		},

		redrawTree : function() {
			var len = adhocAnalysis.data.sortedTagArray.length;
			// only modifiy the count
			this.redrawCountOnly([adhocAnalysis.ALLTAGID,
					adhocAnalysis.CERTIFIEDTAGID, adhocAnalysis.AUDITEDTAGID]);
			/*
			 * var allNode = { id : adhocAnalysis.ALLTAGID, name : 'ALL',
			 * description : 'All', count : adhocAnalysis.data.filterCount };
			 * var certifiedNode = { id : adhocAnalysis.CERTIFIEDTAGID, name :
			 * 'CERTIFIED', description : 'Certified', count :
			 * adhocAnalysis.data.certifiedCount }; // add all and certified on
			 * the top of the tree var treeHtml = '<li>' +
			 * this.drawANode(allNode) + '</li>'; treeHtml += '<li>' +
			 * this.drawANode(certifiedNode) + '</li>';
			 */
			var treeHtml = "";
			treeHtml += this.recursiveDrawTreeNode(0, len - 1);
			this.tree.empty();
			this.tree.append('<ul class="nav nav-list">' + treeHtml + '</ul>');
			this.tree.treeview();
		},

		redrawCountOnly : function(tagIdArr) {
			var len = tagIdArr.length, i;

			for (i = 0; i < len; i++) {
				var tagId = tagIdArr[i];
				var countSpan = $("a.tag[name='" + tagId + "']")
						.find("span.filterCount");
				countSpan.empty();
				countSpan.append(this.getFilterCountHtmlByTagId(tagId));
			}
		},

		getFilterCountHtmlByTagId : function(tagId) {
			var count = 0;
			if (tagId == adhocAnalysis.ALLTAGID) {
				count = adhocAnalysis.data.filterCount;
			} else if (tagId == adhocAnalysis.CERTIFIEDTAGID) {
				count = adhocAnalysis.data.certifiedCount;
			} else if (tagId == adhocAnalysis.AUDITEDTAGID) {
				count = adhocAnalysis.data.auditedCount;
			} else {
				count = adhocAnalysis.data.tags[tagId].count;
			}
			return "(" + count + ")";
		},

		getTreeNodeByTag : function(tag) {
			var att, index;
			for (att in adhocAnalysis.data.tags) {
				if (adhocAnalysis.data.tags.hasOwnProperty(att)) {
					if (adhocAnalysis.data.tags[att].tag == tag) {
						var node = {};
						node.id = adhocAnalysis.data.tags[att].id;
						node.tag = adhocAnalysis.data.tags[att].tag;
						node.description = adhocAnalysis.data.tags[att].description;

						index = node.tag.lastIndexOf('.');
						node.name = node.tag.substring(index + 1);
						if (index > 0) {
							node.parentTag = node.tag.substring(0, index);
						}
						node.count = adhocAnalysis.data.tags[att].count;
						node.title = "";
						if (node.description) {
							node.title = node.description;
						} else if (node.name) {
							node.title = node.name;
						}

						node.editable = true;
						if (adhocAnalysis.data.tags[att].system) {
							node.userDefineTag = false;
						} else {
							node.userDefineTag = true;
						}

						return node;
					}
				}
			}
		},

		drawANode : function(thisNode) {
			var nodeHtml = "";
			nodeHtml += '<a href=""  class="tag" title="' + thisNode.tag
					+ '" name="' + thisNode.id + '">';
			if (thisNode.userDefineTag) {
				nodeHtml += thisNode.title;
			} else {
				nodeHtml += '<strong>' + thisNode.title + '</strong>';
			}

			nodeHtml += '<span class="filterCount"> (' + thisNode.count
					+ ')</span>';
			if (thisNode.userDefineTag) {
				nodeHtml += '<span class="pull-right hide deleteTag" title="Remove"><i class="icon-remove"></i></span>';
			}
			nodeHtml += '<span class="pull-right hide editTag" title="Edit"><i class="icon-edit"></i></span>';
			nodeHtml += '</a>';
			return nodeHtml;
		},

		recursiveDrawTreeNode : function(startIndex, endIndex) {
			if (startIndex > endIndex) {
				return '';
			}
			var treeHtml = '<li>', i, thisNode = this
					.getTreeNodeByTag(adhocAnalysis.data.sortedTagArray[startIndex]);
			treeHtml += this.drawANode(thisNode);
			for (i = startIndex + 1; i <= endIndex; i++) {
				var nextNode = this
						.getTreeNodeByTag(adhocAnalysis.data.sortedTagArray[i]);
				// next sibeling
				if (thisNode.parentTag && nextNode && nextNode.parentTag
						&& nextNode.parentTag == thisNode.parentTag) {
					break;
				} else if (!thisNode.parentTag && !nextNode.parentTag) {
					// both has null parent
					break;
				}
			}
			if (startIndex + 1 <= i - 1) {
				treeHtml += '<ul class="nav nav-list">';
				// draw children
				treeHtml += this.recursiveDrawTreeNode(startIndex + 1, i - 1);
				treeHtml += '</ul>';
			}
			treeHtml += '</li>';
			// draw siebling
			treeHtml += this.recursiveDrawTreeNode(i, endIndex);
			return treeHtml;
		}
	},

	filterTab : {
		tabContainer : null,
		filterListTab : null,
		editfilterTab : null,
		closeFilterDetailBtn : null,

		showFilterListTab : function() {
			this.filterListTab.tab('show');
		},

		showEditFilterTab : function() {
			// show edit page
			this.editfilterTab.tab('show');
			this.tabContainer.show();
		},

		hideEditFilterTab : function() {
			this.filterListTab.tab('show');
			this.tabContainer.hide();
		},

		init : function() {
			var scope = this;
			scope.tabContainer = $("#filterTab");
			scope.filterListTab = $("#filterTab a:first");
			scope.editfilterTab = $("#filterTab a:last");
			scope.closeFilterDetailBtn = $("#editFilterTabClose");

			scope.showFilterListTab();
			scope.closeFilterDetailBtn.click(function(e) {
						e.stopPropagation();
						scope.hideEditFilterTab();
					});
		}
	},

	confirmDeleteModal : {
		modal : null,
		confirmDeleteBtn : null,
		init : function() {
			var scope = this;
			scope.modal = $("#confirmDeleteModal");
			scope.confirmDeleteBtn = $("#confirmDeleteBtn");
			scope.confirmDeleteBtn.click(function() {
						scope.modal.modal('hide');
						adhocAnalysis.filterTable.deleteQuery();
					});
		},
		show : function() {
			this.modal.modal('show');
		}
	},

	filterTable : {
		MAX_DESCRIPTION_LEN : 120,
		dataTable : null,
		data : null,
		modifiedRowTR : null,
		deleteRowTR : null,
		deleteQueryId : null,
		addNewFilterButton : null,
		addNewDashboardBtn : null,

		init : function() {
			var scope = this;
			scope.dataTable = $('#filtersTbl').dataTable({
				"sDom" : "<'row-fluid'<'span2'l><'span4'<'addNewFilterDiv'>><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
				"sPaginationType" : "bootstrap",
				"aLengthMenu" : [[5, 10, 25, 50, 100, -1],
						[5, 10, 25, 50, 100, "All"]],
				"iDisplayLength" : 50,
				"bDeferRender": true,
				"aoColumns" : [{
							"bSortable" : false
						}, null,null, null, null, null,null,null,null]
			});

			$(".addNewFilterDiv")
					.html('<div class="dropdown">'
							+ '<button data-toggle="dropdown" class="btn btn-link dropdown-toggle"><img src="/public/images/ico/new-file-icon.png"></button>'
							+ '<ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">'
							+ '<li><a id="addNewFilterBtn" href="#">New Query Report</a></li>'
							+ '<li><a id="addNewDashboardBtn" href="#">New Dashboard Report</a></li>'
							+ '</ul>' + '</div>');

			scope.addNewFilterButton = $("#addNewFilterBtn");
			scope.addNewDashboardBtn = $('#addNewDashboardBtn');

			// bind table row event
			scope.dataTable.on("click", "tbody tr td button.editQuery", null,
					function(e) {
						e.preventDefault();
						var btn = e.currentTarget;
						scope.loadEditQuery(btn.value, $(btn));
						e.stopPropagation();
					});
			scope.dataTable.on("click", "tbody tr td button.deleteQuery", null,
					function(e) {
						e.preventDefault();
						var btn = e.currentTarget;
						scope.beforeDeleteQuery(btn.value, $(btn));
						e.stopPropagation();
					});
			scope.dataTable.on("mouseenter", "tbody tr", null, function(e) {
						$(this).find('button').show();
					});

			scope.dataTable.on("mouseleave", "tbody tr", null, function(e) {
						$(this).find('button').hide();
					});

			scope.dataTable.on("click", "tbody tr", null, function(e) {
				var queryId = $(this).find('input[name="trQueryId"]').val();
				var isDashboard=$(this).find('input[name="trDashboard"]').val();
				var url = "/adhoc/datatable";
				if (isDashboard=='true') {
					url="/adhoc/dashboard";
				}
				
				if (queryId > 0) {
					url += "?queryId=" + queryId;
					if (adhocAnalysis.tagTree.currentTagId) {
						url += "&tagId=" + adhocAnalysis.tagTree.currentTagId;
					}
					// run this query
					window.location = url;
					// reload this filter for last run result to refresh
					// on UI
					/*var urlQuery = "/adhoc/queryJson?queryId=" + queryId;

					setTimeout(function() {
						$.ajax({
							type : "get",
							url : urlQuery,
							dataType : "json",
							success : function(filter, textStatus) {
								// find the old records
								var len = adhocAnalysis.data.filters.length;
								var found = false, beforeFilter;
								for (var i = 0; i < len; i++) {
									if (adhocAnalysis.data.filters[i].id == filter.id) {
										// swap them
										found = true;
										beforeFilter = adhocAnalysis.data.filters[i];
										adhocAnalysis.data.filters[i] = filter;
										break;
									}
								}
								if (found) {
									$.event
											.trigger(
													adhocAnalysis.events.filterDataChange,
													[beforeFilter, filter]);
								}
							}
						});
					}, 10000);*/
				}

			});

			scope.addNewFilterButton.click(function() {
						adhocAnalysis.filterForm.isDashboard = false;
						scope.addNewReport();
					});

			scope.addNewDashboardBtn.click(function() {
						adhocAnalysis.filterForm.isDashboard = true;
						scope.addNewReport();
					});
		},

		addNewReport : function(isDashboard) {
			this.modifiedRowTR = null;
			adhocAnalysis.filterForm.clearForm(adhocAnalysis.filterForm);

			// populate default tag id{
			if (adhocAnalysis.tagTree.currentTagId
					&& adhocAnalysis.data.tags[adhocAnalysis.tagTree.currentTagId]) {
				adhocAnalysis.filterForm
						.appendNewTag(adhocAnalysis.data.tags[adhocAnalysis.tagTree.currentTagId].tag);
			} else if (adhocAnalysis.data.defaultRootTag) {
				// default one
				adhocAnalysis.filterForm
						.appendNewTag(adhocAnalysis.data.defaultRootTag);
			}
			// query based report only
			if (!adhocAnalysis.filterForm.isDashboard) {
				// populate default agg value
				adhocAnalysis.filterForm.inputs.aggregations[0].method
						.val("COUNT");
				adhocAnalysis.filterForm.inputs.aggregations[0].col.val("*");
				adhocAnalysis.filterForm.inputs.aggregations[0].label
						.val("total records");
				adhocAnalysis.filterForm.showQueryBasedOnly();
				adhocAnalysis.filterForm.texts.reportType.append(adhocAnalysis.REPORTTYPE_Q);
			} else {
				// populate default dashboard definiction
				var defaultJson = '{"columns":3,"type":"SECTIONS","containers":[]}';
				adhocAnalysis.filterForm.inputs.dashboardDefinition
						.val(defaultJson);
				adhocAnalysis.filterForm.showDashboardBasedOnly();
				adhocAnalysis.filterForm.texts.reportType.append(adhocAnalysis.REPORTTYPE_D);
			}
			
		
		  

			adhocAnalysis.filterTab.showEditFilterTab();
		},

		loadEditQuery : function(queryId, element) {
			this.modifiedRowTR = element.closest("td").closest("tr")[0];
			var url = "/adhoc/queryJson";
			if (queryId > 0) {
				url += "?queryId=" + queryId;
				$.ajax({
							type : "get",
							url : url,
							dataType : "json",
							success : function(json, textStatus) {
								adhocAnalysis.filterForm.populateForm(json,
										this.filterForm);
								adhocAnalysis.filterTab.showEditFilterTab();
							}
						});
			}
		},

		beforeDeleteQuery : function(queryId, element) {
			this.deleteRowTR = element.closest("td").closest("tr")[0];
			this.deleteQueryId = queryId;
			adhocAnalysis.confirmDeleteModal.show();
		},

		deleteQuery : function() {
			var url = "/adhoc/deleteQuery";
			$.ajax({
				type : "post",
				url : url,
				data : {
					queryId : adhocAnalysis.filterTable.deleteQueryId
				},
				dataType : "json",
				context : this,
				success : function(json, textStatus) {
					if (json.deletedId) {
						adhocAnalysis.deleteAFilter(json.deletedId);
					} else {
						// show error text
						adhocAnalysis
								.errorAlert("Cannot delete this report due error");
					}

				}
			});

		},

		redraw : function(tagId) {
			// clear it
			this.dataTable.fnClearTable();
			this.addRows(tagId);
		},

		addRow : function(filter) {
			if (filter) {
				this.dataTable.fnAddData(this.getRowDataFromJson(filter));
			}
		},

		updateRow : function(filter, aRow) {
			this.dataTable.fnUpdate(this.getRowDataFromJson(filter), aRow,
					null, false, false);
		},

		addRows : function(tagId) {
			var filterList = adhocAnalysis.data.filters;
			var len = filterList.length;
			var tableRows=[];
			for (var att = 0; att < len; att++) {
				if ((tagId == adhocAnalysis.ALLTAGID)
						|| (tagId == adhocAnalysis.CERTIFIEDTAGID && filterList[att].certified)
						|| (tagId == adhocAnalysis.AUDITEDTAGID && filterList[att].audited)
						|| adhocAnalysis.filterHasTag(filterList[att], tagId)) {
					tableRows.push(this.getRowDataFromJson(filterList[att]));
				}
			}
			this.dataTable.fnAddData(tableRows);
		},
		
		formateShortDate : function (dateStr){
			var aDate=new Date(dateStr);
			var month=aDate.getMonth()+1;
			var date=+aDate.getDate();
			var year=aDate.getFullYear();
			
			if (month<10){
				month='0'+month;
			}
			if (date<10){
				date='0'+date;
			}
			return month+'/'+date+'/'+year;
			
		},

		getRowDataFromJson : function(json) {
			var certified = '', name = '', id = '', alert = '', description = '', action = '', dateModified = '', dateLastRun = '', des = null, desAbbr = null,reportType='Q',createdBy='',modifiedBy='',dateCreated='';
			id = '<input type="hidden" name="trQueryId" value="' + json.id
					+ '">';
		    id+='<input type="hidden" name="trDashboard" value="' + json.dashboard
					+ '">';
			if (json.dashboard){
				reportType='D';
			}
			if (json.dateCreated){
				dateCreated=this.formateShortDate(json.dateCreated);
			}
			
			if (json.createdBy){
				createdBy=json.createdBy;
			}
			
			if (json.modifiedBy){
				modifiedBy=json.modifiedBy;
			}
			
			if (json.certified == true) {
				certified = '<img src="/public/images/ico/certified-icon.png" title="Linnaeus Certified">';

			}
			if (json.audited) {
				if (json.alertMessage) {
					alert = '<img src="/public/images/ico/alert_icon.png" title="'
							+ 'Assertion errors: ' + json.alertMessage + '">';
				} else {
					alert = '<img src="/public/images/ico/audit_icon.png" title="Audited">';
				}
			}
			if (json.queryName) {
				name = json.queryName;
			}
			if (json.dateModified) {
				dateModified = this.formateShortDate(json.dateModified);
			}
			if (json.dateLastRun) {
				dateLastRun =this.formateShortDate(json.dateLastRun);
			}
			/*
			 * if (json.groupName) { col3 = json.groupName; }
			 */
			des = json.description;
			if (des) {
				if (des.length > this.MAX_DESCRIPTION_LEN) {
					desAbbr = des.substring(0, this.MAX_DESCRIPTION_LEN)
							+ '...';
				}
				if (desAbbr) {
					description = '<abbr title="' + des + '">' + desAbbr
							+ '</abbr>';
				} else {
					description = json.description;
				}
			}

			if (json.certified != true) {
				action += '<button class="btn  btn-mini btn-link deleteQuery pull-right hide" value="'
						+ json.id
						+ '" title="Remove"><i class="icon-remove"></i></a>';
			}

			action += '<button class="btn  btn-mini btn-link editQuery pull-right hide" value="'
					+ json.id
					+ '" title="Edit"><i class="icon-edit" value="'
					+ json.id + '"></i></button>';
			return [id + certified + alert, reportType,name + action, description,
					dateLastRun, dateModified,modifiedBy,dateCreated,createdBy];
		},

		highligtRow : function(element) {
			// $('#filtersTbl tbody tr.info').removeClass('info');
			element.closest("tbody").find("tr.info").removeClass('info');
			element.closest("td").closest("tr").addClass('info');
		}
	},

	filterForm : {
		queryTextAreaHeight : 250,
		queryTextArealineHeight : 21,
		queryTextAreaLengthPerLine : 70, // rought estimation

		isDashboard : false,

		inputs : {
			queryName : null,
			id : null,
			query : null,
			description : null,
			tag : null,
			aggregations : [],
			dashboardDefinition : null
		},
		
		texts :{
		    reportType : null,
			modifiedBy : null,
			dateModified : null,
			createdBy : null,
			dateCreated : null,
			dateLastRun : null
		},

		checkboxes : {
			certified : null
		},

		buttons : {
			save : null,
			test : null,
			saveRun : null,
			addNewAgg : null,
			expandAdditionalInfo : null,
			collapseAdditionalInfo : null
		},

		helps : {
			helpInlines : null,
			helpBlocks : null
		},
		legends : {
			requiredFieldLegend : null,
			certifiedQueryLegend : null
		},
		controlGroups : {
			query : null,
			queryName : null,
			description : null,
			tag : null,
			aggregations : null,
			dashboardDefinition : null

		},
		
	

		validationErrors : {
			query : null,
			queryName : null,
			description : null,
			tag : null,
			aggregations : null,
			dashboardDefinition : null
		},

		tagsDiv : null,
		aggDiv : null,
		addtionalInfoDiv : null,

		getQueryAutoCompleteOffSet : function() {
			var offset = Math
					.ceil((this.inputs.query.caret().start / this.queryTextAreaLengthPerLine))
					* this.queryTextArealineHeight + 3;
			if (offset > this.queryTextAreaHeight) {
				offset = this.queryTextAreaHeight;
			}
			return offset;
		},

		showQueryBasedOnly : function() {
			$("div.queryBased").show();
			$("div.dashboardBased").hide();
		},

		showDashboardBasedOnly : function() {
			$("div.queryBased").hide();
			$("div.dashboardBased").show();
		},

		saveReport : function(runQueryAfter) {
			var postData = this.populateSaveJson();
			if (this.validateFilterForm('save', postData)) {
				var url = "/adhoc/saveQuery";
				$.ajax({
					type : "post",
					url : url,
					data : postData,
					dataType : "json",
					success : function(json, textStatus) {
						adhocAnalysis.addOrModifyAFilter(json);
						if (runQueryAfter) {
							var url = "/adhoc/datatable";
							var queryId = json.filter.id;
							if (json.filter.dashboard){
								url="/adhoc/dashboard";
							}
							if (queryId > 0) {
								url += "?queryId=" + queryId;
								if (adhocAnalysis.tagTree.currentTagId)
									url += "&tagId="
											+ adhocAnalysis.tagTree.currentTagId;
								// run this query
								window.location = url;
							}
						}
					},
					error : function() {
						adhocAnalysis
								.errorAlert("Error add or modifiy a report.");
					}
				});
			}
		},

		init : function() {
			var scope = this;
			scope.inputs.queryName = $("#inputQueryName");
			scope.inputs.id = $("#hiddenQueryId");
			scope.inputs.description = $("#inputDescription");
			scope.inputs.groupName = $("#inputQueryGroup");
			scope.inputs.query = $("#inputQuery");
			scope.inputs.tag = $("#selectQueryTag");
			scope.inputs.dashboardDefinition = $("#inputDashboradDefinition");
			
			scope.texts.reportType=$("#filterForm_reportType");
			scope.texts.createdBy=$("#filterForm_createdBy");
			scope.texts.dateCreated=$("#filterForm_dateCreated");
			scope.texts.modifiedBy=$("#filterForm_modifiedBy");
			scope.texts.dateModified=$("#filterForm_dateModified");
			scope.texts.dateLastRun=$("#filterForm_dateLastRun");
			

			scope.checkboxes.certified = $("#certifiedCheckBox");

			scope.buttons.save = $("#saveQueryBtn");
			scope.buttons.test = $("#runQueryBtn");
			scope.buttons.saveRun = $("#saveRunQueryBtn");
			scope.buttons.addNewAgg = $("#addNewAggBtn");
			scope.buttons.expandAdditionalInfo = $("#filterForm_showAdditionalInfo_btn");
			scope.buttons.collapseAdditionalInfo = $("#filterForm_hideAdditionalInfo_btn");
	

			scope.helps.helpInlines = $("#filterForm span.help-inline");
			scope.helps.helpBlocks = $("#filterForm span.help-block");

			scope.legends.requiredFieldLegend = $("div.requiredFieldLegend");
			scope.legends.certifiedQueryLegend = $("div.certifiedQueryLegend");

			scope.validationErrors.query = scope.inputs.query
					.next("span.validationError");
			scope.controlGroups.query = scope.inputs.query
					.closest("div.control-group");

			scope.validationErrors.queryName = scope.inputs.queryName
					.next("span.validationError");
			scope.controlGroups.queryName = scope.inputs.queryName
					.closest("div.control-group");

			scope.validationErrors.description = scope.inputs.description
					.next("span.validationError");
			scope.controlGroups.description = scope.inputs.description
					.closest("div.control-group");

			scope.validationErrors.tag = scope.inputs.tag
					.next("span.validationError");
			scope.controlGroups.tag = scope.inputs.tag
					.closest("div.control-group");

			scope.validationErrors.dashboardDefinition = scope.inputs.dashboardDefinition
					.next("span.validationError");
			scope.controlGroups.dashboardDefinition = scope.inputs.dashboardDefinition
					.closest("div.control-group");

			scope.controlGroups.aggregations = $("div.control-group.aggregations");
			scope.validationErrors.aggregations = $("span.validationError.aggregations");

			scope.tagsDiv = $("#tagsDiv");
			scope.aggDiv = $("#aggDiv");
			scope.addtionalInfoDiv=$("#filterForm_additionalInfo_div");

			// init first row of data
			var aggRow = $("div.aggRow");
			scope.initAggregationRow(aggRow, 0);

			// bind event
			scope.buttons.test.click(function(e) {
						e.preventDefault();
						scope.testQuery();
					});

			scope.inputs.tag.keydown(function(e) {
						if (e.keyCode == 13) {
							e.preventDefault();
							scope.onEnterNewTag();
						};
					});

			scope.tagsDiv.on("click", "a.removeTag", null, function(e) {
						e.preventDefault();
						$(e.currentTarget).closest("div").remove();
					});

			scope.buttons.save.click(function(e) {
						e.preventDefault();
						scope.saveReport(false);
					});

			scope.buttons.saveRun.click(function(e) {
						e.preventDefault();
						scope.saveReport(true);
					});

			scope.buttons.addNewAgg.click(function(e) {
						e.preventDefault();
						scope.addNewAggregationRows(1);
					});

			scope.aggDiv.on("click", "button.removeAggBtn", null, function(e) {
						e.preventDefault();
						var removeBtn = $(e.currentTarget), currentRow;
						var addBtn = scope.buttons.addNewAgg;
						addBtn.detach();
						currentRow = removeBtn.closest("div.controls").remove();
						// remove the data as well
						delete scope.inputs.aggregations[removeBtn.val()];
						// append to the last row
						$("div.aggRow:last").append(addBtn);
					});
			scope.buttons.expandAdditionalInfo.click(function (e){
				e.preventDefault();
				scope.addtionalInfoDiv.show();
				$(this).hide();
				scope.buttons.collapseAdditionalInfo.show();
				
			});
			
			scope.buttons.collapseAdditionalInfo.click(function (e){
				e.preventDefault();
				scope.addtionalInfoDiv.hide();
				$(this).hide();
				scope.buttons.expandAdditionalInfo.show();
				
			});

			scope.inputs.query
					// don't navigate away from the field on tab when selecting
					// an item
					.bind("keydown", function(event) {
						if (event.keyCode === $.ui.keyCode.TAB
								&& $(this).data("autocomplete").menu.active) {
							event.preventDefault();
						}
					}).autocomplete({
						minLength : 6, // start after select
						source : function(request, response) {
							// current cusor index
							var cursorIndex = scope.inputs.query.caret().start;
							var val = request.term.substring(0, cursorIndex);
							var indexS = val.lastIndexOf(" ");
							var indexD = val.lastIndexOf(".");
							var index = Math.max(indexS, indexD);

							var availableTags = function() {
								if (index == indexS) {
									// it is space, return table name
									return adhocAnalysis.getAvailQueryTbls();
								} else if (index == indexD) {
									// it is . get the table name
									var tableName = val.substring(indexS + 1,
											indexD);
									return adhocAnalysis
											.getAvailQueryCols(tableName);
								} else {
									return [];
								}

							}

							var extractLast = function() {
								return (val.substring(index + 1, cursorIndex));
							}
							// delegate back to autocomplete, but extract the
							// last term
							response($.ui.autocomplete.filter(availableTags(),
									extractLast()));
						},
						focus : function() {
							// prevent value inserted on focus
							return false;
						},
						select : function(event, ui) {
							var cursorIndex = scope.inputs.query.caret().start;
							var value = scope.inputs.query.val();
							var removeLast = function(val) {
								// current cusor index
								var strval = val.substring(0, cursorIndex);
								var indexS = strval.lastIndexOf(" ");
								var indexD = strval.lastIndexOf(".");
								var index = Math.max(indexS, indexD);
								return (strval.substring(0, index + 1));
							}
							var remainingList = function(val) {

								return (val.substring(cursorIndex));
							}
							var firstPart = removeLast(value);
							var itemValue = ui.item.value;
							var remainPart = remainingList(value);
							scope.inputs.query.val(firstPart + itemValue + ','
									+ remainPart);
							var newCursorIndex = firstPart.length
									+ itemValue.length + 1;
							scope.inputs.query.caret(newCursorIndex,
									newCursorIndex);
							return false;
						},
						open : function(event, ui) {
							// dynamic position it
							$(this).autocomplete("option", "position", {
								my : 'left top',
								at : 'left top',
								collision : 'none',
								offset : '0 '
										+ scope.getQueryAutoCompleteOffSet()
							});
						},
						position : {
							my : 'left top',
							at : 'left top',
							offset : '0 ' + scope.getQueryAutoCompleteOffSet(),
							collision : 'none'
						}
					}).data("autocomplete")._renderItem = function(ul, item) {
				return $("<li></li>")
						.data("item.autocomplete", item)
						.append('<a href="#"><div class="row"><div class="span4">'
								+ item.value
								+ '</div>'
								+ '<div class="span1"><small>'
								+ item.type
								+ '</small></div>'
								+ '<div class="span4"><small>'
								+ item.description
								+ '</small></div>'
								+ '<div class="span4"><small>'
								+ ("undefined" === typeof(item.table)
										? ' '
										: item.table)
								+ '</small></div></div></a>').appendTo(ul);
			};

			scope.populateTagSelect();

		},

		initAggregationRow : function(row, index) {
			// init aggregation row
			this.inputs.aggregations[index] = {};
			this.inputs.aggregations[index].id = row
					.children("input[name='hiddenAggId']");
			this.inputs.aggregations[index].id.val("");
			this.inputs.aggregations[index].method = row
					.children("select[name='inputAggMethod']");
			this.inputs.aggregations[index].method.val("");
			this.inputs.aggregations[index].col = row
					.children("input[name='inputAggCol']");
			this.inputs.aggregations[index].col.val("");
			this.inputs.aggregations[index].label = row
					.children("input[name='inputAggLabel']");
			this.inputs.aggregations[index].label.val("");
			this.inputs.aggregations[index].alertMethod = row
					.children("select[name='inputAggAlertMethod']");
			this.inputs.aggregations[index].alertMethod.val("");
			this.inputs.aggregations[index].alertValue = row
					.children("input[name='inputAggThreshold']");
			this.inputs.aggregations[index].alertValue.val("");
		},

		addNewAggregationRows : function(rowCount, queryCertified) {
			var i, btn = this.buttons.addNewAgg, newAggRow, removeBtn, index;
			for (i = 0; i < rowCount; i++) {
				index = this.inputs.aggregations.length;
				btn.detach();
				// clone a new row from the row 0
				newAggRow = $("div.aggRow:first").clone();
				// add it into the input array
				this.initAggregationRow(newAggRow, index);
				// do not generate remove button if query is certified
				if (!queryCertified) {
					removeBtn = '<button class="removeAggBtn btn btn-link" title="Remove" value="'
							+ index + '"><i class="icon-minus"></i></button>';
					this.aggDiv.append(newAggRow.append(removeBtn).append(btn));
				} else {
					this.aggDiv.append(newAggRow.append(btn));
				}

			}
		},

		onEnterNewTag : function() {
			var scope = this, val = $.trim(scope.inputs.tag.val());
			// tag can not start or end with .
			if (val && val.charAt(0) != "."
					&& val.charAt(val.length - 1) != ".") {
				scope.appendNewTag(val);
				scope.inputs.tag.val("");
			}
		},

		appendNewTag : function(val, filterCertified) {
			if (val) {
				val = val.toUpperCase();
				// split it into sub string
				var tags = val.split(".");
				if ($.inArray(tags[0], adhocAnalysis.data.sortedTagArray) < 0
						&& adhocAnalysis.data.defaultRootTag) {
					// not found , append the default root tag
					val = adhocAnalysis.data.defaultRootTag + "." + val;
				}
				var html = '<div><span class="label label-info reportTag">'
						+ val + '</span>';
				// if certified, still can delete tag button
				// if (!filterCertified) {
				html += '<a href="#" class="btn btn-mini btn-link removeTag" title="Remove"><i class="icon-remove"></i></a></div>';
				// }
				this.tagsDiv.append(html);
			}
		},

		populateTagSelect : function() {
			this.inputs.tag.autocomplete({
				source : function(request, response) {
					response($.ui.autocomplete.filter(
							adhocAnalysis.data.sortedTagArray, request.term));
				},
				change : function() {
					adhocAnalysis.filterForm.onEnterNewTag();
				}
			}).data("autocomplete")._renderItem = function(ul, item) {
				return $("<li></li>")
						.data("item.autocomplete", item)
						.append('<a href="#"><div class="row"><div class="span4">'
								+ item.value
								+ '</div><div class="span2"><small>'
								+ adhocAnalysis.getTagTitleByName(item.value)
								+ '</small></div></div></a>').appendTo(ul);
			};
		},

		setGroupValue : function(groupVal) {
			this.inputs.groupName.val(groupVal);
		},

		disableFormForEdit : function(scope) {
			if (!scope) {
				scope = this;
			}
			var p, t, i, len;
			for (p in scope.inputs) {
				if (p != 'aggregations') {
					// do not disable description and tag
					if (scope.inputs.hasOwnProperty(p) && p != 'description'
							&& p != 'tag') {
						scope.inputs[p].attr("disabled", "true");
					}
				} else {
					len = scope.inputs.aggregations.length;
					for (i = 0; i < len; i++) {
						for (t in scope.inputs.aggregations[i]) {
							if (scope.inputs.aggregations[i].hasOwnProperty(t)) {
								scope.inputs.aggregations[i][t].attr(
										"disabled", "true");
							}
						}
					}
				}
			}
			for (p in scope.checkboxes) {
				if (scope.checkboxes.hasOwnProperty(p)) {
					scope.checkboxes[p].attr("disabled", "true");
				}
			}
			// hide button
			/*
			 * for (p in scope.buttons) { if (scope.buttons.hasOwnProperty(p)) {
			 * scope.buttons[p].hide(); } }
			 */
			scope.buttons.addNewAgg.hide();

			/*
			 * // hide helps for (p in scope.helps) { if
			 * (scope.helps.hasOwnProperty(p)) { scope.helps[p].hide(); } }
			 */
			scope.legends.requiredFieldLegend.hide();
			scope.legends.certifiedQueryLegend.show();
		},

		clearForm : function(scope) {
			if (!scope) {
				scope = this;
			}
			// remove extra agg
			scope.buttons.addNewAgg.detach();
			$("div.aggRow:first").append(scope.buttons.addNewAgg);
			$("div.aggRow:gt(0)").remove();

			// remove extra agg data
			scope.inputs.aggregations = scope.inputs.aggregations.slice(0, 1);

			var p, i;
			for (p in scope.inputs) {
				if (p != "aggregations") {
					if (scope.inputs.hasOwnProperty(p)) {
						scope.inputs[p].val("");
						scope.inputs[p].removeAttr('disabled');
					}
				} else {
					for (p in scope.inputs.aggregations[0]) {
						scope.inputs.aggregations[0][p].val("");
						scope.inputs.aggregations[0][p].removeAttr('disabled');
					}
				}
			}
			
			for (p in scope.texts){
				if (scope.texts.hasOwnProperty(p)){
					scope.texts[p].empty();
				}
			}

			for (p in scope.checkboxes) {
				if (scope.checkboxes.hasOwnProperty(p)) {
					scope.checkboxes[p].removeAttr('checked');
					scope.checkboxes[p].removeAttr('disabled');
				}
			}

			// show button
			for (p in scope.buttons) {
				if (scope.buttons.hasOwnProperty(p) ) {
					if  (p !='collapseAdditionalInfo'){
						scope.buttons[p].show();
					}else if (p =='collapseAdditionalInfo'){
						scope.buttons[p].hide();
					}
				}
			}

			// show helps
			for (p in scope.helps) {
				if (scope.helps.hasOwnProperty(p)) {
					scope.helps[p].show();
				}
			}
			scope.addtionalInfoDiv.hide();
			// clear tags
			scope.tagsDiv.empty();

			scope.legends.requiredFieldLegend.show();
			scope.legends.certifiedQueryLegend.hide();

			scope.clearValidationErrors(scope);

		},

		clearValidationErrors : function(scope) {
			if (!scope) {
				scope = this;
			}
			// empty validation errors
			for (p in scope.validationErrors) {
				if (scope.validationErrors.hasOwnProperty(p)) {
					scope.validationErrors[p].empty();
				}
			}
			// remove errors from control group
			for (p in scope.controlGroups) {
				if (scope.controlGroups.hasOwnProperty(p)) {
					scope.controlGroups[p].removeClass("error");
				}
			}
		},

		validateFilterForm : function(action, formData) {
			var scope = this;
			var requiredMessage = adhocAnalysis.errorMsgs.requiredMsg;
			var invaidFilterNameMessage = 'Invalid report name. Report name cannot contain # and space.';
			var duplicateFilterNameMessage = 'Report name is used already. Report name must be unique.';

			scope.clearValidationErrors(scope);

			var valid = true;
			var queryName = formData.queryName;
			var query = formData.query;
			var description = formData.description;
			var queryId = formData.id;
			var dashboardDef = formData.dashboardDefinition;
			var aggs = formData.aggregations, aggsLen, i, aggErrorMsg = '';

			if (!formData.isDashboard) {
				if (!query) {
					scope.validationErrors.query.append(requiredMessage);
					scope.controlGroups.query.addClass("error");
					valid = false;
				}
			} else {
				if (!dashboardDef) {
					//console.log(scope.validationErrors.dashboardDefinition);
					scope.validationErrors.dashboardDefinition
							.append(requiredMessage);
					scope.controlGroups.dashboardDefinition.addClass("error");
					valid = false;
				}

			}
			if (action == 'save') {
				if (!queryName) {
					scope.validationErrors.queryName.append(requiredMessage);
					scope.controlGroups.queryName.addClass("error");
					valid = false;
				} else {
					if (queryName.indexOf("#") > 0
							|| queryName.indexOf(" ") > 0) {
						scope.validationErrors.queryName
								.append(invaidFilterNameMessage);
						scope.controlGroups.queryName.addClass("error");
						valid = false;
					}
				}
				if (!description) {
					scope.validationErrors.description.append(requiredMessage);
					scope.controlGroups.description.addClass("error");
					valid = false;
				}
				if (!formData.isDashboard) {
					// validate aggregations
					aggsLen = formData.aggregations.length;
					for (i = 0; i < aggsLen; i++) {
						if (formData.aggregations[i].method
								&& formData.aggregations[i].method.length > 0) {
							if (!formData.aggregations[i].col
									&& formData.aggregations[i].col.length <= 0) {
								aggErrorMsg += formData.aggregations[i].method
										+ ": column is required.";
							}
							if (!formData.aggregations[i].label
									&& formData.aggregations[i].label.length <= 0) {
								aggErrorMsg += formData.aggregations[i].method
										+ ": label is required.";
							}
						}
						if (formData.aggregations[i].alertMethod
								&& formData.aggregations[i].alertMethod.length > 0) {
							if (!formData.aggregations[i].alertValue
									&& formData.aggregations[i].alertValue <= 0) {
								aggErrorMsg += formData.aggregations[i].alertMethod
										+ ": alert value is required.";
							} else if (!$
									.isNumeric(formData.aggregations[i].alertValue)) {
								aggErrorMsg += formData.aggregations[i].alertMethod
										+ ": alert value is not a valid number.";
							}
						}
					}
					if (aggErrorMsg && aggErrorMsg.length > 0) {
						scope.validationErrors.aggregations.append(aggErrorMsg);
						scope.controlGroups.aggregations.addClass("error");
						valid = false;
					}
				}
				/*
				 * if (!tags || tags.length <= 0) {
				 * scope.validationErrors.tag.append(requiredMessage);
				 * scope.controlGroups.tag.addClass("error"); valid = false; }
				 */

				if (valid) {
					// back end validation, filter name need to be unique
					var postData = {};
					postData.queryName = queryName;
					postData.queryId = queryId;
					$.ajax({
						type : "post",
						data : postData,
						async : false,
						url : "/adhoc/validateQueryName",
						dataType : "text",
						success : function(text, textStatus) {
							if ("false" == text) {
								scope.validationErrors.queryName
										.append(duplicateFilterNameMessage);
								scope.controlGroups.queryName.addClass("error");
								valid = false;
							}
						}
					});
				}
			}
			return valid;
		},

		populateForm : function(json, scope) {
			if (!scope) {
				scope = this;
			}
			scope.clearForm(scope);
			// populate tag value
			if (json.tags) {
				var i, len = json.tags.length;
				for (i = 0; i < len; i++) {
					this
							.appendNewTag(
									adhocAnalysis.data.tags[json.tags[i].map.tagId].tag,
									json.certified);
				}
			}

			var inputs = scope.inputs;
			var checkboxes = scope.checkboxes;
			var texts=scope.texts;
			
			var p;
			for (p in inputs) {
				if (p =='dashboardDefinition'){
					inputs[p].val(json.query);
				}
				else if (p == 'aggregations') {
					if (json.aggregations) {
						// populate agg value
						var i, len = json.aggregations.length, att;
						if (len > 1) {
							this.addNewAggregationRows(len - 1, json.certified);
						}
						for (i = 0; i < len; i++) {
							for (att in inputs.aggregations[i]) {
								if (inputs.aggregations[i].hasOwnProperty(att)) {
									inputs.aggregations[i][att]
											.val(json.aggregations[i][att]);
								}
							}
						}
					}
					
				}else {
					if (inputs.hasOwnProperty(p)) {
						inputs[p].val(json[p]);
					}
				}
			}
			
			for (p in texts) {
				if (p !='reportType'){
					if (texts.hasOwnProperty(p)) {
						texts[p].append(json[p]);
					}
				}
			}
			
			for (p in checkboxes) {
				if (checkboxes.hasOwnProperty(p) && json[p] === true) {
					checkboxes[p].attr("checked", "true");
				}
			}
			if (json.certified === true) {
				scope.disableFormForEdit(scope);
			}
			
			if (json.dashboard){
				scope.isDashboard=true;
				scope.showDashboardBasedOnly();
				scope.texts.reportType.append(adhocAnalysis.REPORTTYPE_D);
			}else{
				scope.isDashboard=false;
				scope.showQueryBasedOnly();
				scope.texts.reportType.append(adhocAnalysis.REPORTTYPE_Q);
			}

		},
		populateSaveJson : function() {
			var scope = this;
			var postData = {};
			postData.tags = [];
			
			postData.isDashboard=this.isDashboard;

			var inputs = scope.inputs, checkboxes = scope.checkboxes, aggs = scope.inputs.aggregations, aggLen = aggs.length, p, i, index = 0;

			// populate tag values different
			for (p in inputs) {
				if (p != "tag" && p != "aggregations"
						&& inputs.hasOwnProperty(p)) {
					//console.log(p);
					//console.log($.trim(inputs[p].val()));
					postData[p] = $.trim(inputs[p].val());
				}
				if (p == "aggregations") {
					postData.aggregations = [];
					for (i = 0; i < aggLen; i++) {
						// sparse array,some elmenent is deleted
						if (aggs[i]) {
							postData.aggregations[index] = {};
							for (p in aggs[i]) {
								if (aggs[i].hasOwnProperty(p)) {
									postData.aggregations[index][p] = $
											.trim(aggs[i][p].val());
								}
							}
							index++;
						}
					}
					postData.aggregationLength = postData.aggregations.length;
				}
			}
			for (p in checkboxes) {
				if (checkboxes.hasOwnProperty(p)) {
					if (checkboxes[p].is(':checked')) {
						postData[p] = "true";
					}
				}
			}

			// populate tags
			// check the input as well
			scope.onEnterNewTag();
			scope.tagsDiv.find("span.reportTag").each(function() {
				        var tagVal;
				        tagVal=$(this).text();
						if ($.inArray(tagVal, postData.tags) < 0) {
							postData.tags.push(tagVal);
						}
					});
			return postData;
		},

		testQuery : function() {
			var scope = this,url;
			var postData = scope.populateSaveJson();
			postData.testOnly='true';
			if (scope.validateFilterForm(null, postData)) {
				var win = window.open("", 'result');
				url="/adhoc/datatable";
				if (scope.isDashboard){
					url="/adhoc/dashboard";
				}
				$.ajax({
							type : "POST",
							url : url,
							data : postData,
							success : function(data) {
								win.document.write(data);
								win.document.close();
							}
						})
			}else{
				return false;
			}
		}
	}
};

$(function() {
			adhocAnalysis.init();
		});
