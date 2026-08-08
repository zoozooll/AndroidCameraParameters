import React, {useState, useRef, useEffect} from 'react';
import clsx from 'clsx';
import useIsBrowser from '@docusaurus/useIsBrowser';
import {translate} from '@docusaurus/Translate';
import IconLightMode from '@theme/Icon/LightMode';
import IconDarkMode from '@theme/Icon/DarkMode';
import IconSystemColorMode from '@theme/Icon/SystemColorMode';

import styles from './styles.module.css';

export default function ColorModeToggle({className, buttonClassName, value, onChange}) {
  const isBrowser = useIsBrowser();
  const dropdownRef = useRef(null);
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!dropdownRef.current || dropdownRef.current.contains(event.target)) {
        return;
      }
      setShowDropdown(false);
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('touchstart', handleClickOutside);
    document.addEventListener('focusin', handleClickOutside);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('touchstart', handleClickOutside);
      document.removeEventListener('focusin', handleClickOutside);
    };
  }, [dropdownRef]);

  const items = [
    {
      value: 'light',
      label: translate({
        message: 'Light',
        id: 'theme.colorToggle.ariaLabel.mode.light',
      }),
      icon: <IconLightMode className={styles.menuIcon} />,
    },
    {
      value: 'dark',
      label: translate({
        message: 'Dark',
        id: 'theme.colorToggle.ariaLabel.mode.dark',
      }),
      icon: <IconDarkMode className={styles.menuIcon} />,
    },
    {
      value: null,
      label: translate({
        message: 'System',
        id: 'theme.colorToggle.ariaLabel.mode.system',
      }),
      icon: <IconSystemColorMode className={styles.menuIcon} />,
    },
  ];

  const currentItem = items.find((item) => item.value === value) || items[2];

  return (
    <div
      ref={dropdownRef}
      className={clsx('dropdown', 'dropdown--hoverable', styles.dropdown, className, {
        'dropdown--show': showDropdown,
      })}>
      <button
        type="button"
        className={clsx('clean-btn', styles.toggleButton, buttonClassName)}
        onClick={() => setShowDropdown(!showDropdown)}
        aria-haspopup="true"
        aria-expanded={showDropdown}
        disabled={!isBrowser}
        title={currentItem.label}>
        {currentItem.icon}
      </button>
      <ul className={clsx('dropdown__menu', 'dropdown__menu--right', styles.dropdownMenu)}>
        {items.map((item) => (
          <li key={item.value ?? 'system'}>
            <button
              type="button"
              className={clsx('dropdown__link', {
                'dropdown__link--active': item.value === value,
              }, styles.dropdownItem)}
              onClick={() => {
                onChange(item.value);
                setShowDropdown(false);
              }}>
              {item.icon}
              {item.label}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
