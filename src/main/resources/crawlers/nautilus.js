{
  site: {
    start: 'http://nautil.us/issues/',
    next: [
        (url) => url.indexOf('http://nautil.us/issues/page/' === 0),
        (url) => url.indexOf('http://nautil.us/issue/') === 0)
    ],
    page: (url) => url.match(new Regexp('http://nautil.us/issue/[0-9]+/[^/]*/[^/]*'))
  },
  data: {
    speakerName_ss: null,
    audio_url_s: () => null,
    transcript_s: () => null,
    talk_year_i: null,
    tags_ss: null,
    description_s: null,
    collection_ss: ['Nautilus'],
    url_s: url_s
  }
}