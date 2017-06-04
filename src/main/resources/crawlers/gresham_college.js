{
  site: {
    start: 'https://www.gresham.ac.uk/watch/',
    next: (url) => url.indexOf('https://www.gresham.ac.uk/watch/?page=' === 0)
    page: (url) => url.match('lectures-and-events')
  },
  data: {
    speakerName_ss: () => {
      return $('.speaker-name').map(
        function() {
          return $(this).text().trim()
        }
      )
    },
    audio_url_s: () => {
      return $('.audio-player a').attr('href')
    },
    transcript_s: () => {
      return $('.transcript-text-content p[style*="text-align: justify"]').text()
    },
    talk_year_i: () => {
      return $('.sidebar-block-header')
        .text()
        .match('\\b\\d\\d\\d\\d\\b')[0]
    },
    tags_ss: () => $('.tags a').map(
      function() {
        return $(this).text().trim()
      }
    ),
    description_s: () => $('.copy p').text(),
    collection_ss: ['Gresham College'],
    url_s: url_s
  }
}