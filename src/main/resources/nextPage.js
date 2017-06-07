const fs = require('graceful-fs');
const _ = require('lodash');
const crawler = require('./crawlers/' + process.argv[2]);
const filename = process.argv[3];

const str = fs.readFileSync(filename);
const $ = require('cheerio').load(str);

console.log(crawler.nextPage($));