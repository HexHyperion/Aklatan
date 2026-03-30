// ==UserScript==
// @name         Bookland ISBN scrapper
// @namespace    http://tampermonkey.net/
// @version      2026-03-27
// @description  Automatically scrapes ISBNs of books sold by Bookland; the page can't be used normally with this script active.
// @author       You
// @match        https://bookland.com.pl/*
// @icon         data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==
// @grant        GM_xmlhttpRequest
// @connect      localhost
// ==/UserScript==

(() => {
    'use strict';

    function waitForElement(selector) {
        return new Promise(resolve => {
            if (document.querySelector(selector)) {
                return resolve(document.querySelector(selector));
            }

            const observer = new MutationObserver(mutations => {
                if (document.querySelector(selector)) {
                    observer.disconnect();
                    resolve(document.querySelector(selector));
                }
            });

            observer.observe(document.body, {
                childList: true,
                subtree: true
            });
        });
    }

    function patchHistoryMethod(method) {
        const original = history[method];
        history[method] = function (...args) {
            const result = original.apply(this, args);
            window.dispatchEvent(new Event("locationchange"));
            return result;
        };
    }

    patchHistoryMethod("pushState");
    patchHistoryMethod("replaceState");

    const isbnDataSelector = ".ProductCard-MiddleContent > div > div > p:first-child > span";
    const nextPageButtonSelector = '.PaginationLink[aria-label="Następna strona"]';

    function scrapIsbns() {
        setTimeout(() => {
            waitForElement(isbnDataSelector).then(elem => {
                const isbns = Array.from(document.querySelectorAll(isbnDataSelector), node => node.innerText.trim());
                console.log("Read ISBNs:", isbns);

                GM.xmlHttpRequest({
                    method: "POST",
                    url: "http://localhost:3000/",
                    headers: { "Content-Type": "application/json" },
                    data: JSON.stringify({ isbns: isbns }),
                    onload: (res) => {
                        const nextPageButton = document.querySelector(nextPageButtonSelector);
                        if (nextPageButton) {
                            console.log("Found next page button:", nextPageButton);
                            nextPageButton.click();
                        } else {
                            console.log("No more pages found!");
                        }
                    },
                    onerror: (err) => console.error("Error sending ISBNs:", err),
                })
            });
        }, 2000);
    }

    window.addEventListener("locationchange", scrapIsbns);

    scrapIsbns();
})();