
let priorCaption = '00:00:00,000';

module.exports = {
  site: {
    domain: 'https://www.heavybit.com',
    start: 'https://www.heavybit.com/library/',
    //start: "https://www.heavybit.com/library/video/google-dev-evangelist-on-http-2-0/",
    // curl 'https://www.heavybit.com/library/page/2/' -H 'pragma: no-cache' -H 'x-newrelic-id: VQUGUVRUDBABU1ZUDwgFVw==' -H 'accept-encoding: gzip, deflate, sdch, br' -H 'x-requested-with: XMLHttpRequest' -H 'accept-language: en-US,en;q=0.8' -H 'user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36' -H 'accept: text/html, */*; q=0.01' -H 'cache-control: no-cache' -H 'authority: www.heavybit.com' -H 'cookie: __cfduid=d22cb73b8357588a58c48c803b9e4cf141496796432; ajs_anonymous_id=%22ae89ead4-4d82-45ca-be9e-2a0948083591%22; etBloomCookie_optin_2=true; muxData=mux_viewer_id=9e9a9d3e-8dda-4d6d-a176-8fca00cc9d44&msn=0.7703704554286686&sid=4c6e7c8e-4626-4447-bc94-6afde862fe6d&sst=1496796517780&sex=1496799529088; ajs_user_id=null; ajs_group_id=null' -H 'referer: https://www.heavybit.com/library/' --compressed
    nextPage: 'https://www.heavybit.com/library/page/{page}/',
    dataPage: '/library/video/',
    maxPage: 16
  },
  page: ($) => { $('.heavybit-entry a').attr('href') },
  data: {
    speakerName_ss: ($) => [$("meta[property='og:title']").attr('content').split(" | ")[0]],
    title_s: ($) => $("meta[property='og:title']").attr('content').split(" | ")[1],
    description_s: ($) => $("meta[property='og:description']").attr('content'),
    tag_s: ($) => [$("meta[property='article:tag']").attr('content')],
    url_s: ($) => $("meta[property='og:url']").attr('content'),
    captions: ($) => $('#transcript span[begin]').map(
      function() {
        const begin = $(this).attr('begin');
        const result = priorCaption + ' --> ' + begin + ' ' + $(this).text()
        priorCaption = begin;

        return result;
      }
    ).get(),
    talk_year_i: ($) => {
        const value = $("meta[property='article:published_time']").attr('content');
        return parseInt(value.match(/\b\d\d\d\d\b/)[0]);
    },
    audio_length_f: ($) => {
        const all_times = $('#transcript span[begin]').map(
          function() {
            return [$(this).attr('begin'), $(this).text()];
          }
        ).get().filter(
            (x) => x && x.length > 1 && !!x[0] && !!x[1]
        );

        if (all_times.length > 0) {
            const time = all_times[all_times.length - 2];

            const parts = time.split(':');
            const last = parts[2].split(".");

            const length =
                3600 * parseInt(parts[0]) + 60 * parseInt(parts[1]) +
                      parseInt(last[0]) + parseInt(last[1]) / 1000.0;

            return length;
        } else {
            return null;
        }
    },
    video_url_s: ($) => $($('video source').get()[0]).attr('src'),
    category_l1_ss: ['Business'],
    category_l1_ss: ['Business'],
    features_ss: ['Video'],
    talk_type_l1_ss: ['Conference'],
    talk_type_l2_Conference_ss: ['Business'],
    talk_type_l3_Business_ss: ['Heavybit'],
    collection_l1_ss: ['Heavybit']
  }
}
