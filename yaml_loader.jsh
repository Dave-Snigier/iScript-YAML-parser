// Loads YAML formatted config files onto a global CONFIG object
// allows for using YAML formatted configuration files in leu of editing the scripts each time
// created: 10/11/2012
// @author Dave Snigier (UMass) dsnigier@gmail.com
// @param {string} path directory or file path, can be relative to intool or absolute
// @param {boolean} optional - recursive Look through subdirectories as well for files that match. Default is false
// @returns {boolean} returns true if path was valid, false otherwise
// @example
// // relative path with a single file
// loadYAMLConfig('../script/somescript/config.yaml');
//
// // absolute path with a directory, looking for config files recursively
// loadYAMLConfig('/export/ditstwebimg01/inserver6/script/somescript/', true);

// Load dependencies
// include the YAML library
#include "$IMAGENOWDIR6$\\script\\lib\\yaml\\Yaml.js"
#include "$IMAGENOWDIR6$\\script\\lib\\yaml\\YamlInline.js"
#include "$IMAGENOWDIR6$\\script\\lib\\yaml\\YamlParseException.js"
#include "$IMAGENOWDIR6$\\script\\lib\\yaml\\YamlParser.js"

#include "$IMAGENOWDIR6$\\script\\lib\\yaml\\YamlUnescaper.js"
#include "$IMAGENOWDIR6$\\script\\lib\\yaml\\YamlDumper.js"
#include "$IMAGENOWDIR6$\\script\\lib\\yaml\\YamlEscaper.js"

// include Perceptive code
#include "$IMAGENOWDIR6$\\script\\lib\\spoolFileToBuffer.jsh"

// include Dave code
#include "$IMAGENOWDIR6$\\script\\lib\\expandTemplate.jsh"

function loadYAMLConfig(path, recursive) {
	var results = [];

	// are we going to look recursively thorugh directories. Defaults to false
	recursive = (typeof recursive === 'undefined') ? false : recursive;

	// check to see if path is a file
	if (testForYamlExtension(path)) {
		// is probably a file
		results = SElib.directory(path, false);
		// if SElib.directory returns no results or is invalid it will return a null object
		if (!results || results.length === 0) {
			debug.log('WARNING', "loadYAMLConfig: Not a valid file [%s]\n", path);
			return false;
		}
		parseYaml(results[0].name);
	} else {
		// is probably a directory
		// get immediate directory name
		var re = /^.*\\(.*)\\/;
		var dirName = path.replace(re, '$1');
		//results = SElib.directory(path + "*.yaml", recursive);
		results = SElib.directory(path+"*.yaml", true);
		// if SElib.directory returns no results or is invalid it will return a null object
		if (!results || results.length === 0) {
			debug.log('WARNING', "loadYAMLConfig: Path contains no config files or is invalid (did you remember to put a trailing '/'?) [%s]\n", path);
			return false;
		}
		for (var i = results.length - 1; i >= 0; i--) {
			debug.log('DEBUG', 'loadYAMLConfig: Loading config [%s]\n', results[i].name);
			parseYaml(results[i].name, dirName);
		}
	}

	// checks if file path has a yaml extension based on parsing the filename only
	// @private
	// @param {string} input
	// @returns {boolean} true if there's a match, false otherwise
	function testForYamlExtension(input) {
		// matching regex looking for strings ending in .yaml
		var re = /^.*\.yaml$/;
		return re.test(input);
	}

	// loads a yaml configuration file into memory, parses the text and places the object on global.CONFIG[config file name]
	// @private
	// @param {string} path full path to the yaml file to be parsed
	// @param {string} prefix (optional) - object to prefix the assignment with. Default: none
	// @returns {boolean} true if successful, false otherwise
	function parseYaml(path, prefix) {
		// encountered weird out of memory issues when parsing a 9KB YAML file,
		// wrapping this whole thing in a try/catch so we fail grace versus taking down the script that calls this
		try {
			// spool config file to ram
			var fileBuffer = spoolFileToBuffer(path);

			if (!fileBuffer) {
				debug.log("CRITICAL", "unable to load config file at [%s]\n", path);
				return false;
			}

			// attempt to expand macros
			fileBuffer = expandTemplate(fileBuffer);
			if (!fileBuffer) {
				debug.log('CRITICAL', 'Cannot Expand macros in config file at [%s]\n', path);
				return false;
			}
			debug.log('DEBUG', 'after split\n');

			// parse full path to retreive filename without extension
			var re = /^.*[\\\/](.*)\.yaml/;
			var objectName = path.replace(re, '$1');

			// check if global.CFG exists and creates it as an object literal if not
			if (typeof global.CFG === 'undefined') {
				global.CFG = {};
			}
			// check if if a prefix was passed
			if (typeof prefix === 'undefined') {
				debug.log('DEBUG', 'typeof YAML.parse [%s]\n', typeof YAML.parse);
				debug.log('DEBUG', 'typeof YAML [%s]\n', typeof YAML);
				debug.logObject('DEBUG', YAML, 10);
				// parse the YAML and add resulting object to global CFG object
				global.CFG[objectName] = YAML.parse(fileBuffer);
				if (typeof global.CFG[objectName] !== 'object') {
					debug.log('CRITICAL', 'Unable to parse config file. Are you sure this is valid YAML? [%s]', path);
					return false;
				}
			} else {
				if (typeof global.CFG[prefix] === 'undefined') {
					// create object if it doesn't exist
					global.CFG[prefix] = {};
				}

				var attachTo = global.CFG[prefix];

				// parse the YAML and add resulting object to global CFG[prefix] object
				attachTo[objectName] = YAML.parse(fileBuffer);
			}

			if (typeof attachTo[objectName] !== 'object') {
				debug.log('CRITICAL', 'Unable to parse config file. Are you sure this is valid YAML? [%s]', path);
				return false;
			}

			} catch (err) {
				debug.log('CRITICAL', 'Encountered error when parsing YAML config: [%s]\n', err);
				return false;
			}

		return true;
	}
	return true;
}
