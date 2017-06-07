module.exports = {
  site: {
    //start: 'https://www.heavybit.com/library/',
    start: "https://www.heavybit.com/library/video/google-dev-evangelist-on-http-2-0/",
    next: 'https://www.heavybit.com/library/page/{page}/',
    page: '/library/video/'
  },
  data: {
    speakerName_ss: ($) => [$("meta[property='og:title']").attr('content').split(" | ")[0]],
    title_s: ($) => $("meta[property='og:title']").attr('content').split(" | ")[1],
    description_s: ($) => $("meta[property='og:description']").attr('content'),
    tag_s: ($) => [$("meta[property='article:tag']").attr('content')],
    url_s: ($) => $("meta[property='og:url']").attr('content'),
    captions: ($) => $('#transcript span').map(
      function() {
        return $(this).attr('begin') + ' --> ' + $(this).text()
      }
    ).get(),
    talk_year_i: ($) => {
        const value = $("meta[property='article:published_time']").attr('content');
        return parseInt(value.match(/\b\d\d\d\d\b/)[0]);
    },
    audio_length_f: ($) => {
        const all_times = $('#transcript span').map(
          function() {
            return [$(this).attr('begin'), $(this).text()];
          }
        ).get();

        const time = all_times[all_times.length - 2];

        const parts = time.split(':');
        const last = parts[2].split(".");

        const length =
            3600 * parseInt(parts[0]) + 60 * parseInt(parts[1]) +
                  parseInt(last[0]) + parseInt(last[1]) / 1000.0;

        return length;
    },
    video_url_s: ($) => $($('video source').get()[0]).attr('src'),
    collection_ss: ['Heavybit']
  }
}
