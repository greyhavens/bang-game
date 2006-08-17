class CardController < ApplicationController
  model :card
  scaffold :card

  def index
    list
    render :action => 'list'
  end

  # GETs should be safe (see http://www.w3.org/2001/tag/doc/whenToUseGet.html)
  verify :method => :post, :only => [ :destroy, :create, :update ],
         :redirect_to => { :action => :list }

  def list
    @cards = Card.find_all_for_town(current_town)
  end

#   def list
#     @card_pages, @cards = paginate :cards, :per_page => 10
#   end

  def show
    @card = Card.find(params[:id])
  end

  def new
    @card = Card.new
  end

  def create
    @card = Card.new(params[:card])
    if @card.save
      flash[:notice] = 'Card was successfully created.'
      redirect_to :action => 'list'
    else
      render :action => 'new'
    end
  end

  def edit
    @card = Card.find(params[:id])
  end

  def update
    @card = Card.find(params[:id])
    if @card.update_attributes(params[:card])
      flash[:notice] = 'Card was successfully updated.'
      redirect_to :action => 'list'
    else
      render :action => 'edit'
    end
  end

  def destroy
    Card.find(params[:id]).destroy
    redirect_to :action => 'list'
  end

  def current_town
    session[:town] = params[:town] if (params[:town])
    session[:town] ||= "frontier_town"
  end

  def current_view
    session[:view] = params[:view] if (params[:view])
    session[:view] ||= "mech"
  end

  def is_mech
    current_view == "mech"
  end

  def is_viz
    current_view == "viz"
  end

  def town_options
    return [["Frontier Town", "frontier_town"],
            ["Indian Post", "indian_post"],
            ["Boom Town", "boom_town"],
            ["Ghost Town", "ghost_town"],
            ["City of Gold", "city_of_gold"],
           ]
  end
end
