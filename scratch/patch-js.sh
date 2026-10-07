#!/bin/bash
awk '
/const matchPrice = \(price === '"'"'all'"'"' || itemPrice === price\);/ {
    print "                    const itemPriceVal = parseFloat(item.getAttribute(\"data-price-val\")) || 0;"
    print "                    let matchPrice = false;"
    print "                    if (price === \"all\") matchPrice = true;"
    print "                    else if (price === \"free\" && itemPriceVal === 0) matchPrice = true;"
    print "                    else if (price === \"under500\" && itemPriceVal > 0 && itemPriceVal < 500000) matchPrice = true;"
    print "                    else if (price === \"500to1m\" && itemPriceVal >= 500000 && itemPriceVal <= 1000000) matchPrice = true;"
    print "                    else if (price === \"over1m\" && itemPriceVal > 1000000) matchPrice = true;"
    next
}
{ print }
' src/main/resources/templates/index.html > scratch/new_index2.html && mv scratch/new_index2.html src/main/resources/templates/index.html
