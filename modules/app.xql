xquery version "3.0";

module namespace app="http://exist-db.org/apps/stanford-ner/templates";

import module namespace templates="http://exist-db.org/xquery/templates" ;
import module namespace config="http://exist-db.org/apps/stanford-ner/config" at "config.xqm";

declare function app:display-source($node as node(), $model as map(*), $lang as xs:string?, $type as xs:string?) {
    let $source := replace($node/string(), "^\s*(.*)\s*$", "$1")
    let $expanded := replace($source, "@@path", $config:app-root)
    let $eXideLink := templates:link-to-app("http://exist-db.org/apps/eXide", "index.html?snip=" || encode-for-uri($expanded))
    return
        <div xmlns="http://www.w3.org/1999/xhtml" class="source">
            <div class="code" data-language="{if ($lang) then $lang else 'xquery'}">{ $expanded }</div>
            <div class="toolbar">
                <a class="btn run" href="#" data-type="{if ($type) then $type else 'xml'}">Run</a>
                <a class="btn eXide-open" href="{$eXideLink}" target="eXide"
                    data-exide-create="{$expanded}"
                    title="Opens the code in eXide in new tab or existing tab if it is already open.">Edit</a>
                <img class="load-indicator" src="resources/images/ajax-loader.gif"/>
                <div class="output"></div>
            </div>
        </div>
};