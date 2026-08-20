import React from 'react';
import Footer from '@theme-original/BlogPostItem/Footer';
import GiscusComponent from '@site/src/components/GiscusComponent';
import { useBlogPost } from '@docusaurus/plugin-content-blog/client';

export default function FooterWrapper(props) {
  const { isBlogPostPage } = useBlogPost();

  return (
    <>
      <Footer {...props} />
      {isBlogPostPage && <GiscusComponent />}
    </>
  );
}
