// Fix for STOMP / SockJS requiring Node globals
(window as any).global = window;
