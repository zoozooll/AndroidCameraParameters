// @ts-check
// `@type` JSDoc annotations allow editor autocompletion and type checking
// (when paired with `@ts-check`).
// There are various equivalent ways to declare your Docusaurus config.
// See: https://docusaurus.io/docs/api/docusaurus-config

import {themes as prismThemes} from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Android Camera Parameters',
  tagline: 'Advanced Camera2 API Diagnostics Tool for Android',
  favicon: 'img/camera_params_icon.png',

  // Set the production url of your site here
  url: 'https://zoozooll.github.io',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/AndroidCameraParameters/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'zoozooll', // Usually your GitHub org/user name.
  projectName: 'AndroidCameraParameters', // Usually your repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en', 'zh-Hans', 'zh-Hant', 'es', 'pt-BR', 'fr', 'de', 'ru', 'hi', 'id', 'ja', 'ko'],
    localeConfigs: {
      en: { label: 'English' },
      'zh-Hans': { label: '简体中文' },
      'zh-Hant': { label: '繁體中文' },
      es: { label: 'Español' },
      'pt-BR': { label: 'Português (Brasil)' },
      fr: { label: 'Français' },
      de: { label: 'Deutsch' },
      ru: { label: 'Русский' },
      hi: { label: 'हिन्दी' },
      id: { label: 'Bahasa Indonesia' },
      ja: { label: '日本語' },
      ko: { label: '한국어' },
    },
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: './sidebars.js',
          routeBasePath: '/', // Serve the docs at the site's root
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/zoozooll/AndroidCameraParameters/tree/main/website/',
        },
        blog: {
          showReadingTime: true,
          feedOptions: {
            type: ['rss', 'atom'],
            xslt: true,
          },
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/zoozooll/AndroidCameraParameters/tree/main/website/',
          // Useful options to enforce blogging best practices
          onInlineTags: 'warn',
          onInlineAuthors: 'warn',
          onUntruncatedBlogPosts: 'warn',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      // Replace with your project's social card
      image: 'img/AndroidCamera2Params.png',
      metadata: [
        {name: 'keywords', content: 'android, camera2 api, camera parameters, diagnostic tool, developer tools'},
        {name: 'description', content: 'Advanced diagnostic tool for exploring Android Camera2 API capabilities. Inspect sensor details, lens characteristics, and hardware levels for all cameras.'},
      ],
      navbar: {
        title: 'Camera Parameters',
        logo: {
          alt: 'Android Camera Parameters Logo',
          src: 'img/camera_params_icon.png',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Documentation',
          },
          {to: '/blog', label: 'Blog', position: 'left'},
          {
            type: 'localeDropdown',
            position: 'right',
          },
          {
            href: 'https://github.com/zoozooll/AndroidCameraParameters',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Overview',
                to: '/',
              },
              {
                label: 'Development',
                to: '/development',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'Google Play',
                href: 'https://play.google.com/store/apps/details?id=com.minininja.cameraparams',
              },
              {
                label: 'GitHub Issues',
                href: 'https://github.com/zoozooll/AndroidCameraParameters/issues',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'Blog',
                to: '/blog',
              },
              {
                label: 'GitHub',
                href: 'https://github.com/zoozooll/AndroidCameraParameters',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Android Camera Parameters. Built with Docusaurus.`,
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
        additionalLanguages: ['kotlin', 'java', 'json'],
      },
    }),
};

export default config;
