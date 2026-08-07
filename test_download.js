// Simulate what RemoteImage does: download image via OkHttp and decode
const https = require('https');

const url = 'https://api.dreamkoreaubttest.com/wp-content/uploads/2025/11/colorized-illustration-100.jpeg';

https.get(url, (res) => {
  console.log('Status:', res.statusCode);
  console.log('Content-Type:', res.headers['content-type']);
  console.log('Content-Length:', res.headers['content-length']);
  
  const chunks = [];
  res.on('data', chunk => chunks.push(chunk));
  res.on('end', () => {
    const buffer = Buffer.concat(chunks);
    console.log('Downloaded bytes:', buffer.length);
    // Check if it starts with JPEG magic bytes (FF D8 FF)
    console.log('First 4 bytes (hex):', buffer.slice(0, 4).toString('hex'));
    console.log('Is valid JPEG:', buffer[0] === 0xFF && buffer[1] === 0xD8 && buffer[2] === 0xFF);
  });
}).on('error', e => console.error('Error:', e.message));
