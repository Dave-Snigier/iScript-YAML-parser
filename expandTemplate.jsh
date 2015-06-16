// Takes a macro string indicated by ###aVariableHere###
// and expands it to the string contents or representation
// useful for config files and making scripts environment independent
// @author Dave Snigier (UMass), dsnigier@gmail.com
// @param {String} input text to be expanded
// @returns {String|Boolean} text including expansions, false on error
function expandTemplate(input) {
	var re = /###(.+?)###/;
	var offset = 0;
	var result;
	var output = "";
	// find any macros in the text
	while (result = input.substr(offset).match(re)) {
		// add any text between matches
		output += input.substr(offset, result.index);
		// adjust offset
		offset = offset + result.index + result[0].length;
		
		// expand result, catch any errors and return false
		try {
			output += global[result[1]];
		} catch (e) {
			return false;
		}
	}
	// grab the last of the input text
	output += input.substr(offset);
	return output;
}