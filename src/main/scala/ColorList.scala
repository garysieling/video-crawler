import java.io.{File, IOException}
import java.math.BigInteger
import java.nio.charset.Charset
import javax.imageio.ImageIO
import play.api.libs.json.Json

import ij.ImagePlus
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime

/**
  * Created by gary on 11/18/2017.
  */
object ColorList {
  val colors = List("acc2d9", "56ae57", "b2996e", "a8ff04", "69d84f", "894585", "70b23f", "d4ffff", "65ab7c", "952e8f", "fcfc81", "a5a391", "388004", "4c9085", "5e9b8a", "efb435", "d99b82", "0a5f38", "0c06f7", "61de2a", "3778bf", "2242c7", "533cc6", "9bb53c", "05ffa6", "1f6357", "017374", "0cb577", "ff0789", "afa88b", "08787f", "dd85d7", "a6c875", "a7ffb5", "c2b709", "e78ea5", "966ebd", "ccad60", "ac86a8", "947e94", "983fb2", "ff63e9", "b2fba5", "63b365", "8ee53f", "b7e1a1", "ff6f52", "bdf8a3", "d3b683", "fffcc4", "430541", "ffb2d0", "997570", "ad900d", "c48efd", "507b9c", "7d7103", "fffd78", "da467d", "410200", "c9d179", "fffa86", "5684ae", "6b7c85", "6f6c0a", "7e4071", "009337", "d0e429", "fff917", "1d5dec", "054907", "b5ce08", "8fb67b", "c8ffb0", "fdde6c", "ffdf22", "a9be70", "6832e3", "fdb147", "c7ac7d", "fff39a", "850e04", "efc0fe", "40fd14", "b6c406", "9dff00", "3c4142", "f2ab15", "ac4f06", "c4fe82", "2cfa1f", "9a6200", "ca9bf7", "875f42", "3a2efe", "fd8d49", "8b3103", "cba560", "698339", "0cdc73", "b75203", "7f8f4e", "26538d", "63a950", "c87f89", "b1fc99", "ff9a8a", "f6688e", "76fda8", "53fe5c", "4efd54", "a0febf", "7bf2da", "bcf5a6", "ca6b02", "107ab0", "2138ab", "719f91", "fdb915", "fefcaf", "fcf679", "1d0200", "cb6843", "31668a", "247afd", "ffffb6", "90fda9", "86a17d", "fddc5c", "78d1b6", "13bbaf", "fb5ffc", "20f986", "ffe36e", "9d0759", "3a18b1", "c2ff89", "d767ad", "720058", "ffda03", "01c08d", "ac7434", "014600", "9900fa", "02066f", "8e7618", "d1768f", "96b403", "fdff63", "95a3a6", "7f684e", "751973", "089404", "ff6163", "598556", "214761", "3c73a8", "ba9e88", "021bf9", "734a65", "23c48b", "8fae22", "e6f2a2", "4b57db", "d90166", "015482", "9d0216", "728f02", "ffe5ad", "4e0550", "f9bc08", "ff073a", "c77986", "d6fffe", "fe4b03", "fd5956", "fce166", "b2713d", "1f3b4d", "699d4c", "56fca2", "fb5581", "3e82fc", "a0bf16", "d6fffa", "4f738e", "ffb19a", "5c8b15", "54ac68", "89a0b0", "7ea07a", "1bfc06", "cafffb", "b6ffbb", "a75e09", "152eff", "8d5eb7", "5f9e8f", "63f7b4", "606602", "fc86aa", "8c0034", "758000", "ab7e4c", "030764", "fe86a4", "d5174e", "fed0fc", "680018", "fedf08", "fe420f", "6f7c00", "ca0147", "1b2431", "00fbb0", "db5856", "ddd618", "41fdfe", "cf524e", "21c36f", "a90308", "6e1005", "fe828c", "4b6113", "4da409", "beae8a", "0339f8", "a88f59", "5d21d0", "feb209", "4e518b", "964e02", "85a3b2", "ff69af", "c3fbf4", "2afeb7", "005f6a", "0c1793", "ffff81", "f0833a", "f1f33f", "b1d27b", "fc824a", "71aa34", "b7c9e2", "4b0101", "a552e6", "af2f0d", "8b88f8", "9af764", "a6fbb2", "ffc512", "750851", "c14a09", "fe2f4a", "0203e2", "0a437a", "a50055", "ae8b0c", "fd798f", "bfac05", "3eaf76", "c74767", "b9484e", "647d8e", "bffe28", "d725de", "b29705", "673a3f", "a87dc2", "fafe4b", "c0022f", "0e87cc", "8d8468", "ad03de", "8cff9e", "94ac02", "c4fff7", "fdee73", "33b864", "fff9d0", "758da3", "f504c9", "77a1b5", "8756e4", "889717", "c27e79", "017371", "9f8303", "f7d560", "bdf6fe", "75b84f", "9cbb04", "29465b", "696006", "adf802", "c1c6fc", "35ad6b", "fffd37", "a442a0", "f36196", "947706", "fff4f2", "1e9167", "b5c306", "feff7f", "cffdbc", "0add08", "87fd05", "1ef876", "7bfdc7", "bcecac", "bbf90f", "ab9004", "1fb57a", "00555a", "a484ac", "c45508", "3f829d", "548d44", "c95efb", "3ae57f", "016795", "87a922", "f0944d", "5d1451", "25ff29", "d0fe1d", "ffa62b", "01b44c", "ff6cb5", "6b4247", "c7c10c", "b7fffa", "aeff6e", "ec2d01", "76ff7b", "730039", "040348", "df4ec8", "6ecb3c", "8f9805", "5edc1f", "d94ff5", "c8fd3d", "070d0d", "4984b8", "51b73b", "ac7e04", "4e5481", "876e4b", "58bc08", "2fef10", "2dfe54", "0aff02", "9cef43", "18d17b", "35530a", "1805db", "6258c4", "ff964f", "ffab0f", "8f8ce7", "24bca8", "3f012c", "cbf85f", "ff724c", "280137", "b36ff6", "48c072", "bccb7a", "a8415b", "06b1c4", "cd7584", "f1da7a", "ff0490", "805b87", "50a747", "a8a495", "cfff04", "ffff7e", "ff7fa7", "ef4026", "3c9992", "886806", "04f489", "fef69e", "cfaf7b", "3b719f", "fdc1c5", "20c073", "9b5fc0", "0f9b8e", "742802", "9db92c", "a4bf20", "cd5909", "ada587", "be013c", "b8ffeb", "dc4d01", "a2653e", "638b27", "419c03", "b1ff65", "9dbcd4", "fdfdfe", "77ab56", "464196", "990147", "befd73", "32bf84", "af6f09", "a0025c", "ffd8b1", "7f4e1e", "bf9b0c", "6ba353", "f075e6", "7bc8f6", "475f94", "f5bf03", "fffeb6", "fffd74", "895b7b", "436bad", "d0c101", "c6f808", "f43605", "02c14d", "b25f03", "2a7e19", "490648", "536267", "5a06ef", "cf0234", "c4a661", "978a84", "1f0954", "03012d", "2bb179", "c3909b", "a66fb5", "770001", "922b05", "7d7f7c", "990f4b", "8f7303", "c83cb9", "fea993", "acbb0d", "c071fe", "ccfd7f", "00022e", "828344", "ffc5cb", "ab1239", "b0054b", "99cc04", "937c00", "019529", "ef1de7", "000435", "42b395", "9d5783", "c8aca9", "c87606", "aa2704", "e4cbff", "fa4224", "0804f9", "5cb200", "76424e", "6c7a0e", "fbdd7e", "2a0134", "044a05", "fd4659", "0d75f8", "fe0002", "cb9d06", "fb7d07", "b9cc81", "edc8ff", "61e160", "8ab8fe", "920a4e", "fe02a2", "9a3001", "65fe08", "befdb7", "b17261", "885f01", "02ccfe", "c1fd95", "836539", "fb2943", "84b701", "b66325", "7f5112", "5fa052", "6dedfd", "0bf9ea", "c760ff", "ffffcb", "f6cefc", "155084", "f5054f", "645403", "7a5901", "a8b504", "3d9973", "000133", "76a973", "2e5a88", "0bf77d", "bd6c48", "ac1db8", "2baf6a", "26f7fd", "aefd6c", "9b8f55", "ffad01", "c69c04", "f4d054", "de9dac", "05480d", "c9ae74", "60460f", "98f6b0", "8af1fe", "2ee8bb", "11875d", "fdb0c0", "b16002", "f7022a", "d5ab09", "86775f", "c69f59", "7a687f", "042e60", "c88d94", "a5fbd5", "fffe71", "6241c7", "fffe40", "d3494e", "985e2b", "a6814c", "ff08e8", "9d7651", "feffca", "98568d", "9e003a", "287c37", "b96902", "ba6873", "ff7855", "94b21c", "c5c9c7", "661aee", "6140ef", "9be5aa", "7b5804", "276ab3", "feb308", "8cfd7e", "6488ea", "056eee", "b27a01", "0ffef9", "fa2a55", "820747", "7a6a4f", "f4320c", "a13905", "6f828a", "a55af4", "ad0afd", "004577", "658d6d", "ca7b80", "005249", "2b5d34", "bff128", "b59410", "2976bb", "014182", "bb3f3f", "fc2647", "a87900", "82cbb2", "667c3e", "fe46a5", "fe83cc", "94a617", "a88905", "7f5f00", "9e43a2", "062e03", "8a6e45", "cc7a8b", "9e0168", "fdff38", "c0fa8b", "eedc5b", "7ebd01", "3b5b92", "01889f", "3d7afd", "5f34e7", "6d5acf", "748500", "706c11", "3c0008", "cb00f5", "002d04", "658cbb", "749551", "b9ff66", "9dc100", "faee66", "7efbb3", "7b002c", "c292a1", "017b92", "fcc006", "657432", "d8863b", "738595", "aa23ff", "08ff08", "9b7a01", "f29e8e", "6fc276", "ff5b00", "fdff52", "866f85", "8ffe09", "eecffe", "510ac9", "4f9153", "9f2305", "728639", "de0c62", "916e99", "ffb16d", "3c4d03", "7f7053", "77926f", "010fcc", "ceaefa", "8f99fb", "c6fcff", "5539cc", "544e03", "017a79", "01f9c6", "c9b003", "929901", "0b5509", "a00498", "2000b1", "94568c", "c2be0e", "748b97", "665fd1", "9c6da5", "c44240", "a24857", "825f87", "c9643b", "90b134", "01386a", "25a36f", "59656d", "75fd63", "21fc0d", "5a86ad", "fec615", "fffd01", "dfc5fe", "b26400", "7f5e00", "de7e5d", "048243", "ffffd4", "3b638c", "b79400", "84597e", "411900", "7b0323", "04d9ff", "667e2c", "fbeeac", "d7fffe", "4e7496", "874c62", "d5ffff", "826d8c", "ffbacd", "d1ffbd", "448ee4", "05472a", "d5869d", "3d0734", "4a0100", "f8481c", "02590f", "89a203", "e03fd8", "d58a94", "7bb274", "526525", "c94cbe", "db4bda", "9e3623", "b5485d", "735c12", "9c6d57", "028f1e", "b1916e", "49759c", "a0450e", "39ad48", "b66a50", "8cffdb", "a4be5c", "cb7723", "05696b", "ce5dae", "c85a53", "96ae8d", "1fa774", "7a9703", "ac9362", "01a049", "d9544d", "fa5ff7", "82cafc", "acfffc", "fcb001", "910951", "fe2c54", "c875c4", "cdc50a", "fd411e", "9a0200", "be6400", "030aa7", "fe019a", "f7879a", "887191", "b00149", "12e193", "fe7b7c", "ff9408", "6a6e09", "8b2e16", "696112", "e17701", "0a481e", "343837", "ffb7ce", "6a79f7", "5d06e9", "3d1c02", "82a67d", "be0119", "c9ff27", "373e02", "a9561e", "caa0ff", "ca6641", "02d8e9", "88b378", "980002", "cb0162", "5cac2d", "769958", "a2bffe", "10a674", "06b48b", "af884a", "0b8b87", "ffa756", "a2a415", "154406", "856798", "34013f", "632de9", "0a888a", "6f7632", "d46a7e", "1e488f", "bc13fe", "7ef4cc", "76cd26", "74a662", "80013f", "b1d1fc", "ffffe4", "0652ff", "045c5a", "5729ce", "069af3", "ff000d", "f10c45", "5170d7", "acbf69", "6c3461", "5e819d", "601ef9", "b0dd16", "cdfd02", "2c6fbb", "c0737a", "d6b4fc", "020035", "703be7", "fd3c06", "960056", "40a368", "03719c", "fc5a50", "ffffc2", "7f2b0a", "b04e0f", "a03623", "87ae73", "789b73", "ffffff", "98eff9", "658b38", "5a7d9a", "380835", "fffe7a", "5ca904", "d8dcd6", "a5a502", "d648d7", "047495", "b790d4", "5b7c99", "607c8e", "0b4008", "ed0dd9", "8c000f", "ffff84", "bf9005", "d2bd0a", "ff474c", "0485d1", "ffcfdc", "040273", "a83c09", "90e4c1", "516572", "fac205", "d5b60a", "363737", "4b5d16", "6b8ba4", "80f9ad", "a57e52", "a9f971", "c65102", "e2ca76", "b0ff9d", "9ffeb0", "fdaa48", "fe01b1", "c1f80a", "36013f", "341c02", "b9a281", "8eab12", "9aae07", "02ab2e", "7af9ab", "137e6d", "aaa662", "610023", "014d4e", "8f1402", "4b006e", "580f41", "8fff9f", "dbb40c", "a2cffe", "c0fb2d", "be03fd", "840000", "d0fefe", "3f9b0b", "01153e", "04d8b2", "c04e01", "0cff0c", "0165fc", "cf6275", "ffd1df", "ceb301", "380282", "aaff32", "53fca1", "8e82fe", "cb416b", "677a04", "ffb07c", "c7fdb5", "ad8150", "ff028d", "000000", "cea2fd", "001146", "0504aa", "e6daa6", "ff796c", "6e750e", "650021", "01ff07", "35063e", "ae7181", "06470c", "13eac9", "00ffff", "d1b26f", "00035b", "c79fef", "06c2ac", "033500", "9a0eea", "bf77f6", "89fe05", "929591", "75bbfd", "ffff14", "c20078", "96f97b", "f97306", "029386", "95d0fc", "e50000", "653700", "ff81c0", "0343df", "15b01a", "7e1e9c")
  val colorMap =
    colors.map(
      (color) =>
        (
          new BigInteger(color.substring(0, 2), 16).intValue(),
          new BigInteger(color.substring(2, 4), 16).intValue(),
          new BigInteger(color.substring(4, 6), 16).intValue(),
          color
        ))

  val path = "G:\\wikiart\\wikiart\\wikiart\\images"

  def main(args: Array[String]): Unit = {
    import scala.util.matching.Regex
    def recursiveListFiles(f: File, r: Regex): Array[File] = {
      val these = f.listFiles
      val good = these.filter(f => r.findFirstIn(f.getName).isDefined)
      good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_,r))
    }

    val files = recursiveListFiles(new File(path), new Regex(".*"))

    val hex: Map[Long, String] = Map(
      0L -> "0",
      1L -> "1",
      2L -> "2",
      3L -> "3",
      4L -> "4",
      5L -> "5",
      6L -> "6",
      7L -> "7",
      8L -> "8",
      9L -> "9",
      10L -> "A",
      11L -> "B",
      12L -> "C",
      13L -> "D",
      14L -> "E",
      15L -> "F"
    )

    /*val nearest =
      colorMap.map(
        (tuple) =>
          (
            Math.pow(tuple._1 - red, 2) +
            Math.pow(tuple._2 - grn, 2) +
            Math.pow(tuple._3 - blue, 2),
            tuple._4
          )
      ).sortBy(
        (tuple) => tuple._1
      ).head._2*/

    //println(nearest + "(" + red + ", " + grn + ", " + blue + ")")

    //val data =
      files.par.foreach(
        (inputFile: File) => {
          //println(inputFile)
          //println(new DateTime)

          val someImage = ImageIO.read(inputFile)
          val imagePlus = new ImagePlus()
          imagePlus.setImage(someImage)
          val processor = imagePlus.getProcessor()

          val width = processor.getWidth
          val height = processor.getHeight

          //println(new DateTime)

          val colors: List[String] =
            (0 until width).map(
              (x) => (0 until height).map(
                (y) => {
                  val cm = processor.getColorModel
                  val rgb = cm.getRGB(processor.getPixel(x, y))
                  val color: String = (
                    Math.round(cm.getRed(rgb) / 32.0) * 8 * 8 +
                    Math.round(cm.getGreen(rgb) / 32.0) * 8 +
                    Math.round(cm.getBlue(rgb) / 32.0)
                  ) + ""

                  color
                })).flatten.toList

          val dest =
            scala.util.Random.shuffle(
              colors).take(8*8*8*3).mkString(" ")

          println(inputFile.toString.split("\\\\").last + "," + dest)
        }
      )

    //val js: String = Json.stringify(Json.toJson(data))
    //FileUtils.write(new File("G:\\wikiart\\wikiart-importer\\colors.json"), js, Charset.forName("utf8"))

  }
}