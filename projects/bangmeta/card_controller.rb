class CardController < ApplicationController
  model :card
  scaffold :card

  def current_town
    session[:town] = (params[:town] || "frontier_town")
  end

  def current_view
    session[:view] = (params[:view] || "mech")
  end

  def is_mech
    current_view == "mech"
  end

  def is_viz
    current_view == "viz"
  end

  def list
    @cards = Card.find_all_for_town(current_town)
  end

  def town_options
    return [["Frontier Town", "frontier_town"],
            ["Indian Post", "indian_post"]]
  end
end
