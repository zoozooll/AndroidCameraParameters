import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import { useLocation } from '@docusaurus/router';
import BrowserOnly from '@docusaurus/BrowserOnly';
import {
  FacebookShareButton,
  FacebookIcon,
  TwitterShareButton,
  XIcon,
  LinkedinShareButton,
  LinkedinIcon,
  RedditShareButton,
  RedditIcon,
} from 'react-share';

import styles from './styles.module.css';

export default function ShareButtons() {
  const { siteConfig } = useDocusaurusContext();
  const location = useLocation();

  return (
    <BrowserOnly>
      {() => {
        const shareUrl = `${siteConfig.url}${siteConfig.baseUrl}${location.pathname.replace(siteConfig.baseUrl, '')}`;
        const title = siteConfig.title;

        return (
          <div className={styles.shareButtonsContainer}>
            <span className={styles.shareText}>Share:</span>
            <div className={styles.buttons}>
              <FacebookShareButton url={shareUrl} quote={title} className={styles.shareButton}>
                <FacebookIcon size={32} round />
              </FacebookShareButton>

              <TwitterShareButton url={shareUrl} title={title} className={styles.shareButton}>
                <XIcon size={32} round />
              </TwitterShareButton>

              <LinkedinShareButton url={shareUrl} title={title} className={styles.shareButton}>
                <LinkedinIcon size={32} round />
              </LinkedinShareButton>

              <RedditShareButton url={shareUrl} title={title} className={styles.shareButton}>
                <RedditIcon size={32} round />
              </RedditShareButton>
            </div>
          </div>
        );
      }}
    </BrowserOnly>
  );
}
