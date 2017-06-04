{
  site: {
    start: 'https://www.gresham.ac.uk/watch/',
    next: (url) => url.indexOf('https://www.gresham.ac.uk/watch/?page=' === 0)
    page: (url) => url.match('lectures-and-events')
  },
  data: {
    speakerName_ss: null,
    audio_url_s: () => null,
    transcript_s: () => null,
    talk_year_i: null,
    tags_ss: null,
    description_s: null,
    collection_ss: ['Great Black Speakers'],
    url_s: url_s
  }
}