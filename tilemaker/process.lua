function init_function()
end

function safe_attr(key, value)
    if value ~= nil and value ~= "" then
        Attribute(key, value)
    end
end

function node_function()
    local amenity = Find("amenity")
    local shop = Find("shop")
    local tourism = Find("tourism")
    local name = Find("name")

    if amenity ~= "" or shop ~= "" or tourism ~= "" then
        Layer("poi", false)
        safe_attr("name", name)
        safe_attr("amenity", amenity)
        safe_attr("shop", shop)
        safe_attr("tourism", tourism)
    end
end

function way_function()
    local highway = Find("highway")
    local construction = Find("construction")
    local building = Find("building")
    local waterway = Find("waterway")
    local natural = Find("natural")
    local landuse = Find("landuse")
    local leisure = Find("leisure")
    local amenity = Find("amenity")
    local historic = Find("historic")
    local name = Find("name")

    if natural == "water" then
        Layer("water", true)
        safe_attr("name", name)
        safe_attr("class", natural)
    end

    if waterway ~= "" then
        Layer("water_lines", false)
        safe_attr("name", name)
        safe_attr("class", waterway)
    end

    if landuse ~= "" or leisure == "park" or historic ~= "" or amenity == "university" or amenity == "school" then
        Layer("landuse", true)
        safe_attr("name", name)
        safe_attr("class", landuse)
        safe_attr("leisure", leisure)
        safe_attr("historic", historic)
        safe_attr("amenity", amenity)
    end

    if building ~= "" then
        Layer("buildings", true)
        safe_attr("name", name)
        safe_attr("building", building)
    end

    if highway == "construction" or highway == "proposed" then
        Layer("planned_roads", false)
        safe_attr("name", name)
        safe_attr("class", construction)
        safe_attr("opening_date", Find("opening_date"))
        safe_attr("check_date", Find("check_date"))
        return
    end

    if highway ~= "" then
        Layer("roads", false)
        safe_attr("class", highway)
        safe_attr("name", name)
        safe_attr("surface", Find("surface"))
        safe_attr("lanes", Find("lanes"))
        safe_attr("bridge", Find("bridge"))
    end

    if amenity ~= "" then
        Layer("poi", false)
        safe_attr("name", name)
        safe_attr("amenity", amenity)
    end
end