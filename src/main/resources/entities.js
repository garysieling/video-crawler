let fs = require('fs');

let entities = fs.readFileSync('d:\\projects\\scala-indexer\\entities.txt');
let dir = 'c:\\projects\\image-annotation\\data\\talks\\json\\1';

(entities + '').split("\n").map(
  (line) => {
    let parts = line.split(",");
    let token = parts[0];
    let id = parts[1].trim();

    let filename = dir + "\\" + id + ".json";
    console.log(filename)

    let data = JSON.parse(fs.readFileSync(filename, 'utf-8'));

    if (!data.entities) {
      data.entities = [];
    }

    data.entities.push(token);

    fs.writeFileSync(filename, JSON.stringify(data));
  }
)