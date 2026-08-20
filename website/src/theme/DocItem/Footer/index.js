import React from 'react';
import Footer from '@theme-original/DocItem/Footer';
import ShareButtons from '@site/src/components/ShareButtons';
import GiscusComponent from '@site/src/components/GiscusComponent';

export default function FooterWrapper(props) {
  return (
    <>
      <ShareButtons />
      <Footer {...props} />
      <GiscusComponent />
    </>
  );
}
