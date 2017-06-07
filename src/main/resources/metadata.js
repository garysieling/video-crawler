const fs = require('graceful-fs');
const _ = require('lodash');
const crawler = require('./crawlers/' + process.argv[2]);
const filename = process.argv[3];

const str = fs.readFileSync(filename);
const $ = require('cheerio').load(str);

const results =
  _.fromPairs(
    _.map(
      crawler.data,
      (v, k) => {
        console.log(v, k);
        if (_.isFunction(v)) {
          return [k, v($)];
        } else {
          return [k, v];
        }
      }
    )
  );

console.log(results);

console.log(
  JSON.stringify(
    results
  )
);
