const fs = require('graceful-fs');
const _ = require('lodash');
const filename = process.argv[2];

const str = fs.readFileSync(filename);
const $ = require('cheerio').load(str);

const url = require('url');

const data =
  $('a').map(
    (i, el) =>
      url.resolve(
        process.argv[3],
        $(el).attr('href'),
      )
  );

console.log(
  JSON.stringify(
    data.get(),
    null,
    2
  )
);
