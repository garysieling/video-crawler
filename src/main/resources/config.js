const data = require('./crawlers/' + process.argv[2]);
console.log(JSON.stringify(data.site))
