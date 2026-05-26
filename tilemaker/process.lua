function init_function()
end

function node_function(node)
end

function way_function()
    local highway = Find("highway")

    if highway ~= "" then
        Layer("roads", false)
        Attribute("class", highway)

        local name = Find("name")
        if name ~= "" then
            Attribute("name", name)
        end

        local surface = Find("surface")
        if surface ~= "" then
            Attribute("surface", surface)
        end

        local lanes = Find("lanes")
        if lanes ~= "" then
            Attribute("lanes", lanes)
        end
    end
end