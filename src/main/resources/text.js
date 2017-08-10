const htmlToText = require('html-to-text');

const filename = process.argv[2];

htmlToText.fromFile(filename, {
    ignoreHref: true,
    ignoreImages: true
//    tables: ['#invoice', '.address']
}, (err, text) => {
    if (err) return console.error(err);
    console.log(text);
});
