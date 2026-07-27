const express = require('express');
const axios = require('axios');
const cheerio = require('cheerio');

const app = express();

app.get('/api/stream', async (req, res) => {
  const { episodeId, isDub } = req.query;

  if (!episodeId) {
    return res.status(400).json({ error: 'episodeId is required' });
  }

  try {
    const targetUrl = `https://gogoanime.cl/${episodeId}${isDub === 'true' ? '-dub' : ''}`;
    const { data } = await axios.get(targetUrl, {
      headers: { 
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36' 
      }
    });

    const $ = cheerio.load(data);
    const iframeSrc = $('#load_anime iframe').attr('src');

    if (!iframeSrc) {
      return res.status(404).json({ error: 'Stream source iframe not found' });
    }

    const streamIframe = iframeSrc.startsWith('http') ? iframeSrc : `https:${iframeSrc}`;

    res.json({
      success: true,
      streamIframe,
      headers: {
        'Referer': 'https://gogoanime.cl/',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
      }
    });
  } catch (err) {
    res.status(500).json({ error: 'Failed to extract stream', details: err.message });
  }
});

app.get('/ping', (req, res) => res.send('AniGoose Backend Online 🪿'));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
