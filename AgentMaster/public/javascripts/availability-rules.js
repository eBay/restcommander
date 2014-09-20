	$(function () {
		// Sets the value of the field corresponding to a given button
		// it also performs validation on that field
		var setButtonFieldValue = function (inputFieldObj, buttonObj) {
			if (!inputFieldObj || !buttonObj) {
				return false;
			}
		
			var buttonValue = buttonObj.text();
			inputFieldObj.attr("value", buttonValue);
			buttonObj.addClass("active");
			$("#main-form").validate().element(inputFieldObj);
		};
		
		// Rule 1: When selecting COS : QA, only enable zone PHX.
		$("#cos button").click(function () {
			
			var buttonObj = $(this);
			var nonPHXButtons = $("#zone button").filter(function() { return $(this).text() != "PHX" });
			var cosField = $("#selCOS");
			
			var buttonValue = buttonObj.text();
			console.log("cos button clicked = " + buttonValue);
			
			// Copy the value of the button into a corresponding form field.
			setButtonFieldValue(cosField, buttonObj);
			
			// Check for COS = 'QA'
			if (buttonValue == 'QA') {	
				// Show 'PHX' zone only, disable the other 2 zones
				$(nonPHXButtons).attr("disabled", "disabled");
				$(nonPHXButtons).removeClass("active");
				
				// Check the availability zone value field and validate it
				var zoneField = $("#selZone");
				var zoneFieldValue = zoneField.val();
				if (zoneFieldValue != "PHX" && zoneFieldValue != "") {
					zoneField.attr("value", "");
					$("#main-form").validate().element(zoneField);
				}

			} else {
				// Show all zones
				$(nonPHXButtons).removeAttr("disabled", "disabled");
			}
		});

		// Rule 2: When selecting Compute Type = 'Bave Metal', show size 'Standard'; 
		// otherwise (Compute Type = 'Virtual'), show sizes 1 ... size 4
		$("#compute-type button").click(function () {
			var buttonObj = $(this);
			var indexSizes = $("#size button").filter(function() { return $(this).text().match(/Size\s+\d+/i) });
			var standardSize = $("#size button").filter(function() { return $(this).text() == 'Standard' });
			var computeTypeField = $("#selComputeType");
			var buttonValue = buttonObj.text();
			
			// To be used to check the Size field
			var sizeField = $("#selSize");
			var sizeFieldValue = sizeField.val();

			// Copy the value of the button into a corresponding form field.
			setButtonFieldValue(computeTypeField, buttonObj);
			
			if (buttonValue == 'Virtual') {	
				// Disable Standard, enable the rest.
				$(standardSize).attr("disabled", "disabled");
				$(standardSize).removeClass("active");
				$(indexSizes).removeAttr("disabled", "disabled");
				
				// Validate size field based on this selection
				if (sizeFieldValue == "Standard") {
					sizeField.attr("value", "");
					$("#main-form").validate().element(sizeField);
				}
			} else {
				// Enable Standard, disable the rest.
				$(indexSizes).attr("disabled", "disabled");
				$(indexSizes).removeClass("active");
				$(standardSize).removeAttr("disabled", "disabled");
				
				// Validate size field based on this selection
				if (sizeFieldValue != "Standard" && sizeFieldValue != "") {
					sizeField.attr("value", "");
					$("#main-form").validate().element(sizeField);
				}
			}
			
		});

		// Misc. For Validation:
		$("#zone button").click(function () {
			var buttonObj = $(this);
			var inputField = $("#selZone");
			var buttonValue = buttonObj.text();

			// Copy the value of the button into a corresponding form field.
			setButtonFieldValue(inputField, buttonObj);
		});
		
		// Misc. For Validation:
		$("#size button").click(function () {
			var buttonObj = $(this);
			var inputField = $("#selSize");
			var buttonValue = buttonObj.text();

			// Copy the value of the button into a corresponding form field.
			setButtonFieldValue(inputField, buttonObj);
		});
		
		// Misc. For Validation:
		$("#distribution-function button").click(function () {
			var buttonObj = $(this);
			var inputField = $("#selDistFunction");
			var buttonValue = buttonObj.text();

			// Copy the value of the button into a corresponding form field.
			setButtonFieldValue(inputField, buttonObj);
		});

		// Adds the validator to be called when validating the form and
		// then user enters a value on each of the form fields
		$.validator.addMethod(
			"lessThan",
			function(value, element, params) {
				if (!value) {
					return true;
				}

				var target = $(params).val();
				if (!target || target == "") {
					//	$(element).attr('value', '')
				}

				var isValueNumeric = !isNaN(parseFloat(value)) && isFinite(value);
				var isTargetNumeric = !isNaN(parseFloat(target)) && isFinite(target);

				if (isValueNumeric && isTargetNumeric) {
					return Number(value) < Number(target);
				}

				return false;
			}, jQuery.validator.format("The value must be less than the Number of Computes") );

			// Validation Rules for the main form
			$('#main-form').validate({
				rules: {
					selCOS : {
						required: true
					},
					selZone : {
						required: true
					},
					selComputeType : {
						required: true
					},
					selSize : {
						required: true
					},
					numComputes: {
						minlength: 1,
						required: true,
						number: true,
						min: 1
					},
					numFaultDomains: {						
						lessThan: "#numComputes",			
						minlength: 1,
						required: false,
						number: true,
						min: 0
					},
					selDistFunction : {
						required: true
					},
				},
				highlight: function(label) {
					$(label).closest('.control-group').addClass('error');
				},
				success: function(label) {
					label
					.text('OK!').addClass('valid')
					.closest('.control-group').addClass('success');
				}	        
			});
			
			// Handle clicking on the "Check" button
			$("#check-avail").click(function() {
				var isFormValid = $("#main-form").valid();

				if (isFormValid) {
					var msg = requestServer();
					$("#results-container").css('display','block')
					$("#results-data-container").html("<p>" + msg + "</p>");
				}

			});

			/**
		 	 * @method requestServer
			 * @description  Builds the whole JSON and submits an availability check request to the server and wait for sync response
			 * @public
			 * @return {Object} Final message from server
			 */
			function requestServer () {
				var mainForm = "#main-form";

				// Get a hold of all the rules.
				var listRules = $(mainForm).find("input");
				var numRules = listRules.length;
				var ruleKey, ruleValue;
				var resultJson = {};

				// Loop thru the rules
				for (idx = 0; idx < numRules; ++idx) {
					ruleItem = $(listRules[idx]);
					ruleKey = ruleItem.attr('name');
					ruleValue = ruleItem.val();
					resultJson[ruleKey] = ruleValue;
				} // for

				var jsonValue = JSON.stringify(resultJson);
				console.log("The resulting json is:");
				console.log(jsonValue);
				
				// submit an availability check request to the server and wait for sync response
			    var msg = $.ajax({
		        type: "POST",
		        async: false,
		        url: "/capacity/checkavailability",
		        data: jsonValue
		        }).responseText;
			    return msg;
			} // End method requestServer

});