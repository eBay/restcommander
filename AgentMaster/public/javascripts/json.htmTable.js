// This function creates a standard table with column/rows
// Parameter Information
// objArray = Anytype of object array, like JSON results
// theme (optional) = A css class to add to the table (e.g. <table class="<theme>">
// enableHeader (optional) = Controls if you want to hide/show, default is show
function CreateTableView(objArray, theme, enableHeader) {
	// set optional theme parameter
	if (theme === undefined) {
		theme = 'mediumTable'; // default theme
	}

	if (enableHeader === undefined) {
		enableHeader = true; // default enable headers
	}

	var array = typeof objArray != 'object' ? JSON.parse(objArray) : objArray;

	var str = '<table  id="AgentNodeTable" class="' + theme + '">';

	if (array.length == 0) {
		str += '<tr><td> Empty list of data.</td></tr>';
	} else {

		// table head
		if (enableHeader) {
			str += '<thead><tr>';
			str += '<th scope="col">' + 'No.' + '</th>';
			for ( var index in array[0]) {
				str += '<th scope="col">' + index + '</th>';
			}
			str += '</tr></thead>';
		}

		// updated by ypei: added index column ; 0507
		// table body
		str += '<tbody>';

		for ( var i = 0; i < array.length; i++) {
			str += (i % 2 == 0) ? '<tr class="alt">' : '<tr>';
			var j = i + 1;
			str += '<td>' + j + '</td>';
			for ( var index in array[i]) {
				str += '<td>' + array[i][index] + '</td>';
			}
			str += '</tr>';
		}

	}

	str += '</tbody>'
	str += '</table>';
	return str;
}

// This function creates a details view table with column 1 as the header and
// column 2 as the details
// Parameter Information
// objArray = Anytype of object array, like JSON results
// theme (optional) = A css class to add to the table (e.g. <table
// class="<theme>">
// enableHeader (optional) = Controls if you want to hide/show, default is show
function CreateDetailView(objArray, theme, enableHeader) {
	// set optional theme parameter
	if (theme === undefined) {
		theme = 'mediumTable'; // default theme
	}

	if (enableHeader === undefined) {
		enableHeader = true; // default enable headers
	}

	var array = typeof objArray != 'object' ? JSON.parse(objArray) : objArray;

	var str = '<table class="' + theme + '">';
	str += '<tbody>';

	for ( var i = 0; i < array.length; i++) {
		var row = 0;
		for ( var index in array[i]) {
			str += (row % 2 == 0) ? '<tr class="alt">' : '<tr>';

			if (enableHeader) {
				str += '<th scope="row">' + index + '</th>';
			}

			str += '<td>' + array[i][index] + '</td>';
			str += '</tr>';
			row++;
		}
	}
	str += '</tbody>'
	str += '</table>';
	return str;
}
