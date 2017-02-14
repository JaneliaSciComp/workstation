const path = require('path');
const webpack = require('webpack')
const ROOT = path.resolve(__dirname, 'src/main/webapp');
const SRC = path.resolve(ROOT, 'javascript');
const DEST = path.resolve(__dirname, 'src/main/webapp/dist');

module.exports = {
  devtool: 'source-map',
  entry: {
    app: SRC + '/index.jsx',
  },
  resolve: {
    modules: [
      path.resolve(ROOT, 'javascript'),
      path.resolve(ROOT, 'css'),
      'node_modules/'
    ],
    extensions: ['.js', '.jsx', '.css', '.scss']
  },
  output: {
    path: DEST,
    filename: 'bundle.js',
    publicPath: '/dist/'
  },
  module: {
    loaders: [
      {
        test: /\.jsx?$/,
        loader: 'babel-loader',
        query: {
           presets: [
                  'es2015','react'
           ]
        },
        include: SRC
      },
      {
        test: /(\.scss|\.css)$/,
        loader: 'style-loader!css-loader!sass-loader' 
      }
    ]
  }
};
