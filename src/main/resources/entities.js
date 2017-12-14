let fs = require('fs');

let entities = fs.readFileSync('d:\\projects\\scala-indexer\\entities.txt');
let dir = 'c:\\projects\\image-annotation\\data\\talks\\json\\1';
let _ = require('lodash');

(entities + '').split("\n").map(
  (line) => {
    let parts = line.split(",");
    if (parts.length == 2) {
      let token = parts[0];
      let id = parts[1].trim();

      let filename = dir + "\\" + id + ".json";
      console.log(filename)

      let data = JSON.parse(fs.readFileSync(filename, 'utf-8'));

      if (!data.entities_ss) {
        data.entities_ss = [];
      }

      data.entities_ss.push(token);
      data.entities_ss = _.uniq(data.entities_ss);

      fs.writeFileSync(filename, JSON.stringify(data, null, 2));
    }
  }
)